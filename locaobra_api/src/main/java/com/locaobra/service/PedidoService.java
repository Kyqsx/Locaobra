package com.locaobra.service;

import com.locaobra.dto.request.AlocacaoItemRequest;
import com.locaobra.dto.request.ConfirmarPedidoRequest;
import com.locaobra.dto.request.EstimarFreteRequest;
import com.locaobra.dto.request.EnderecoRequest;
import com.locaobra.dto.request.ItemPedidoRequest;
import com.locaobra.dto.request.PedidoDecisaoRequest;
import com.locaobra.dto.request.PedidoRequest;
import com.locaobra.dto.response.FreteEstimativaResponse;
import com.locaobra.dto.response.ItemPedidoResponse;
import com.locaobra.dto.response.PedidoResponse;
import com.locaobra.dto.response.PontoRetiradaResponse;
import com.locaobra.dto.response.SugestaoAlocacaoResponse;
import com.locaobra.entity.*;
import com.locaobra.enums.StatusExpedicao;
import com.locaobra.enums.StatusPedido;
import com.locaobra.enums.StatusUnidade;
import com.locaobra.enums.TipoEntrega;
import com.locaobra.enums.TipoVeiculo;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final DepositoRepository depositoRepository;
    private final UnidadeEquipamentoRepository unidadeRepository;
    private final ExpedicaoRepository expedicaoRepository;
    private final EnderecoRepository enderecoRepository;
    private final EnderecoService enderecoService;
    private final FreteService freteService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ItemPedidoRepository itemRepository,
            ClienteRepository clienteRepository,
            EquipamentoRepository equipamentoRepository,
            FuncionarioRepository funcionarioRepository,
            UsuarioRepository usuarioRepository,
            DepositoRepository depositoRepository,
            UnidadeEquipamentoRepository unidadeRepository,
            ExpedicaoRepository expedicaoRepository,
            EnderecoRepository enderecoRepository,
            EnderecoService enderecoService,
            FreteService freteService) {
        this.pedidoRepository = pedidoRepository;
        this.itemRepository = itemRepository;
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.depositoRepository = depositoRepository;
        this.unidadeRepository = unidadeRepository;
        this.expedicaoRepository = expedicaoRepository;
        this.enderecoRepository = enderecoRepository;
        this.enderecoService = enderecoService;
        this.freteService = freteService;
    }

    // ======================================================================
    // CRIAÇÃO (cliente, pelo catálogo)
    // ======================================================================

    @Transactional
    public PedidoResponse criar(PedidoRequest request) {
        Cliente cliente = resolverClienteLogado();

        if (request.getDataInicio() == null || request.getDataFim() == null) {
            throw new BusinessException("Informe o período da locação (data de início e fim).");
        }
        if (request.getDataFim().isBefore(request.getDataInicio())) {
            throw new BusinessException("A data de fim não pode ser anterior à data de início.");
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new BusinessException("Adicione ao menos um equipamento ao pedido.");
        }

        // ENTREGA (padrão, retrocompatível) ou RETIRADA (endereço dispensado,
        // frete zero). No pedido de retirada o cliente busca no depósito —
        // qual depósito exatamente fica a cargo do consultor na confirmação.
        TipoEntrega tipoEntrega = request.getTipoEntrega() != null
                ? request.getTipoEntrega() : TipoEntrega.ENTREGA;
        boolean retirada = tipoEntrega == TipoEntrega.RETIRADA;

        Endereco enderecoEntrega = null;
        if (!retirada) {
            enderecoEntrega = resolverEnderecoEntrega(cliente, request);
        }

        Pedido pedido = new Pedido();
        pedido.setCodigo(gerarCodigo());
        pedido.setStatus(StatusPedido.SOLICITADO);
        pedido.setCliente(cliente);
        pedido.setDataInicio(request.getDataInicio());
        pedido.setDataFim(request.getDataFim());
        pedido.setTipoEntrega(tipoEntrega);
        pedido.setEnderecoEntrega(enderecoEntrega);
        pedido.setObservacoesCliente(request.getObservacoesCliente());

        long dias = Math.max(1, ChronoUnit.DAYS.between(request.getDataInicio(), request.getDataFim()));
        BigDecimal valorTotal = BigDecimal.ZERO;

        pedido = pedidoRepository.save(pedido);

        for (ItemPedidoRequest itemReq : request.getItens()) {
            if (itemReq.getEquipamentoId() == null) {
                throw new BusinessException("Todo item precisa de um equipamento selecionado.");
            }
            if (itemReq.getQuantidade() == null || itemReq.getQuantidade() < 1) {
                throw new BusinessException("Quantidade inválida para um dos itens.");
            }
            Equipamento equipamento = equipamentoRepository.findById(itemReq.getEquipamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + itemReq.getEquipamentoId()));

            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setEquipamento(equipamento);
            item.setQuantidade(itemReq.getQuantidade());
            item.setValorDiariaSnapshot(equipamento.getValorDiaria());
            item.setObservacaoItem(itemReq.getObservacaoItem());
            itemRepository.save(item);

            valorTotal = valorTotal.add(
                    equipamento.getValorDiaria()
                            .multiply(BigDecimal.valueOf(itemReq.getQuantidade()))
                            .multiply(BigDecimal.valueOf(dias))
            );
        }

        pedido.setValorTotalEstimado(valorTotal);

        // Frete: estimativa na solicitação (origem genérica, sem depósito
        // definido ainda). RETIRADA não tem frete. O valor FINAL é recalculado
        // pelo consultor na confirmação, com o depósito real de origem.
        if (!retirada) {
            pedido.setValorFrete(freteService.calcularFrete(null, null, valorTotal, false));
        }
        pedido = pedidoRepository.save(pedido);

        return construirResponse(pedido);
    }

    // ======================================================================
    // ESTIMATIVA DE FRETE (carrinho, antes de fechar o pedido)
    // ======================================================================

    // Estimativa de frete pra o carrinho: o cliente informa endereço + itens
    // e recebe valor/prazo ANTES de finalizar. Origem: o depósito ativo mais
    // "próximo" do destino (heurística) — a versão final é calculada pelo
    // consultor na confirmação, com o depósito real alocado.
    @Transactional(readOnly = true)
    public FreteEstimativaResponse estimarFrete(EstimarFreteRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new BusinessException("Adicione ao menos um equipamento pra estimar o frete.");
        }

        Endereco destino = resolverEnderecoDestino(request);

        // Peso/dimensões por item (com quantidade) e valor total da locação
        // precisa das datas — no carrinho elas já foram escolhidas.
        if (request.getDataInicio() == null || request.getDataFim() == null) {
            throw new BusinessException("Informe o período da locação pra estimar o frete.");
        }
        long dias = Math.max(1, ChronoUnit.DAYS.between(request.getDataInicio(), request.getDataFim()));

        BigDecimal pesoTotal = BigDecimal.ZERO;
        BigDecimal pesoCubadoTotal = BigDecimal.ZERO;
        BigDecimal valorLocacao = BigDecimal.ZERO;

        for (ItemPedidoRequest itemReq : request.getItens()) {
            if (itemReq.getEquipamentoId() == null) {
                throw new BusinessException("Todo item precisa de um equipamento selecionado.");
            }
            Equipamento eq = equipamentoRepository.findById(itemReq.getEquipamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + itemReq.getEquipamentoId()));

            BigDecimal volume = freteService.volumeM3(
                    eq.getComprimentoCm(), eq.getLarguraCm(), eq.getAlturaCm(), itemReq.getQuantidade());
            pesoTotal = pesoTotal.add(freteService.pesoConsideradoKg(eq.getPesoKg(), volume));
            pesoCubadoTotal = pesoCubadoTotal.add(
                    volume.multiply(freteService.fatorCubagem()).setScale(2, RoundingMode.HALF_UP));
            valorLocacao = valorLocacao.add(eq.getValorDiaria()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantidade()))
                    .multiply(BigDecimal.valueOf(dias)));
        }

        // Origem: depósito ativo mais próximo do destino (heurística de
        // cidade/UF). Sem depósito cadastrado, usa origem desconhecida (25 km).
        Deposito origem = depositoRepository.findByAtivoTrue().stream()
                .min(Comparator.comparingInt(d -> freteService.estimarDistanciaKm(d.getEndereco(), destino)))
                .orElse(null);
        int km = origem != null
                ? freteService.estimarDistanciaKm(origem.getEndereco(), destino)
                : freteService.estimarDistanciaKm(null, destino);

        FreteEstimativaResponse resposta = new FreteEstimativaResponse();
        resposta.setValorFrete(freteService.calcularFrete(pesoTotal, km, valorLocacao, false));
        resposta.setDistanciaKm(km);
        resposta.setPrazoEstimadoDias(freteService.prazoEstimadoDias(km));
        resposta.setPesoTotalKg(pesoTotal);
        resposta.setPesoCubadoKg(pesoCubadoTotal);
        resposta.setVeiculoSugerido(freteService.sugerirVeiculo(pesoTotal));
        return resposta;
    }

    // Resolve o endereço de destino da estimativa: endereço salvo (validando
    // dono) ou digitado na hora — mesmas regras do PedidoRequest.
    private Endereco resolverEnderecoDestino(EstimarFreteRequest request) {
        if (request.getEnderecoId() != null) {
            Cliente cliente = resolverClienteLogado();
            Endereco endereco = enderecoRepository.findById(request.getEnderecoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado: " + request.getEnderecoId()));
            if (endereco.getCliente() == null || !endereco.getCliente().getId().equals(cliente.getId())) {
                throw new BusinessException("Esse endereço não pertence a esse cliente.");
            }
            return endereco;
        }
        EnderecoRequest digitado = request.getEnderecoEntrega();
        if (digitado == null || isBlank(digitado.getCidade())) {
            throw new BusinessException("Informe o endereço de entrega (ao menos cidade e UF) pra estimar o frete.");
        }
        Endereco endereco = new Endereco();
        endereco.setCep(digitado.getCep());
        endereco.setRua(digitado.getRua());
        endereco.setNumero(digitado.getNumero());
        endereco.setComplemento(digitado.getComplemento());
        endereco.setBairro(digitado.getBairro());
        endereco.setCidade(digitado.getCidade());
        endereco.setEstado(digitado.getEstado());
        return endereco;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ======================================================================
    // CONSULTA
    // ======================================================================

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        return construirResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAll().stream().map(this::construirResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status).stream().map(this::construirResponse).collect(Collectors.toList());
    }

    // Fila do consultor: pedidos recém-solicitados, aguardando revisão.
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarFilaConsultor() {
        return listarPorStatus(StatusPedido.SOLICITADO);
    }

    // Fila do conferente: pedidos APROVADO, prontos pra virar
    // expedição. Cada pedido carrega os "grupos pendentes" (um por depósito
    // que ainda não teve expedição gerada) — um pedido desmembrado em 2
    // depósitos só some da fila quando os dois grupos forem atendidos.
    // Se o funcionário logado tem um depósito fixo (Funcionario.deposito),
    // só mostramos os grupos daquele depósito; sem depósito fixo (ex.: admin
    // e gerente de operações), mostramos tudo.
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarFilaConferente() {
        Long depositoDoFuncionario = depositoDoFuncionarioLogadoOuNull();

        List<PedidoResponse> resultado = new ArrayList<>();
        for (Pedido pedido : pedidoRepository.findByStatus(StatusPedido.APROVADO)) {
            List<ItemPedido> itens = itemRepository.findByPedidoId(pedido.getId());
            List<PedidoResponse.GrupoPendenteResponse> grupos = montarGruposPendentes(pedido, itens);

            if (depositoDoFuncionario != null) {
                grupos = grupos.stream()
                        .filter(g -> depositoDoFuncionario.equals(g.getDepositoId()))
                        .collect(Collectors.toList());
            }

            if (grupos.isEmpty()) {
                continue; // nada pendente pra esse conferente — não aparece na fila
            }

            PedidoResponse response = PedidoResponse.from(pedido, itens);
            response.setGruposPendentes(grupos);
            resultado.add(response);
        }
        return resultado;
    }

    // "Meus pedidos" do cliente logado.
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarMeus() {
        Cliente cliente = resolverClienteLogado();
        return pedidoRepository.findByClienteId(cliente.getId()).stream()
                .map(this::construirResponse)
                .collect(Collectors.toList());
    }

    // Pontos de retirada disponíveis pro cliente (depósitos ativos) — usado no
    // carrinho quando ele escolhe RETIRADA. Só expõe nome/endereço.
    @Transactional(readOnly = true)
    public List<PontoRetiradaResponse> listarPontosRetirada() {
        return depositoRepository.findByAtivoTrue().stream()
                .map(PontoRetiradaResponse::from)
                .collect(Collectors.toList());
    }

    // ======================================================================
    // SUGESTÃO DE DEPÓSITO (usada pelo consultor antes de confirmar)
    // ======================================================================

    // Dado um pedido SOLICITADO, sugere de qual depósito cada item deve sair:
    // 1) tenta achar UM depósito que atenda tudo sozinho;
    // 2) se não achar, faz alocação gulosa item a item (cada item vai pro
    //    depósito com mais disponibilidade daquele equipamento);
    // 3) item que nenhum depósito sozinho atende cai em "itensNaoAtendidos",
    //    pro consultor decidir manualmente (ou recusar o pedido).
    @Transactional(readOnly = true)
    public SugestaoAlocacaoResponse sugerirAlocacaoDepositos(Long pedidoId) {
        Pedido pedido = buscarEntidade(pedidoId);
        List<ItemPedido> itens = itemRepository.findByPedidoId(pedido.getId());
        List<Deposito> depositosAtivos = depositoRepository.findByAtivoTrue();

        SugestaoAlocacaoResponse resposta = new SugestaoAlocacaoResponse();
        if (itens.isEmpty() || depositosAtivos.isEmpty()) {
            return resposta;
        }

        // disponibilidade[depositoId][itemId] = quantas unidades disponíveis
        Map<Long, Map<Long, Long>> disponibilidade = new HashMap<>();
        for (Deposito deposito : depositosAtivos) {
            Map<Long, Long> porItem = new HashMap<>();
            for (ItemPedido item : itens) {
                long qtd = unidadeRepository.countByEquipamentoIdAndStatusAndDepositoId(
                        item.getEquipamento().getId(), StatusUnidade.DISPONIVEL, deposito.getId());
                porItem.put(item.getId(), qtd);
            }
            disponibilidade.put(deposito.getId(), porItem);
        }

        // Passo 1: existe um depósito só que atende tudo?
        Deposito depositoUnico = depositosAtivos.stream()
                .filter(d -> itens.stream().allMatch(i -> disponibilidade.get(d.getId()).get(i.getId()) >= i.getQuantidade()))
                .min(Comparator.comparing(Deposito::getNome))
                .orElse(null);

        if (depositoUnico != null) {
            resposta.setAtendeUmDeposito(true);
            resposta.setDepositoUnicoId(depositoUnico.getId());
            resposta.setDepositoUnicoNome(depositoUnico.getNome());

            SugestaoAlocacaoResponse.GrupoDeposito grupo = new SugestaoAlocacaoResponse.GrupoDeposito();
            grupo.setDepositoId(depositoUnico.getId());
            grupo.setDepositoNome(depositoUnico.getNome());
            grupo.setItens(itens.stream().map(i -> paraItemAlocado(i, disponibilidade.get(depositoUnico.getId()).get(i.getId())))
                    .collect(Collectors.toList()));
            resposta.setGrupos(List.of(grupo));
            return resposta;
        }

        // Passo 2: gulosamente, item a item, escolhe o depósito com mais
        // disponibilidade (precisa ser >= quantidade pedida).
        Map<Long, List<ItemPedido>> itensPorDeposito = new LinkedHashMap<>();
        List<SugestaoAlocacaoResponse.ItemNaoAtendido> naoAtendidos = new ArrayList<>();

        for (ItemPedido item : itens) {
            Deposito melhor = null;
            long melhorQtd = -1;
            for (Deposito deposito : depositosAtivos) {
                long qtd = disponibilidade.get(deposito.getId()).get(item.getId());
                if (qtd >= item.getQuantidade() && qtd > melhorQtd) {
                    melhor = deposito;
                    melhorQtd = qtd;
                }
            }

            if (melhor != null) {
                itensPorDeposito.computeIfAbsent(melhor.getId(), k -> new ArrayList<>()).add(item);
            } else {
                // Nenhum depósito sozinho tem o suficiente — reporta o que
                // chegou mais perto, pro consultor decidir.
                Deposito maisProximo = depositosAtivos.stream()
                        .max(Comparator.comparingLong(d -> disponibilidade.get(d.getId()).get(item.getId())))
                        .orElse(null);
                SugestaoAlocacaoResponse.ItemNaoAtendido naoAtendido = new SugestaoAlocacaoResponse.ItemNaoAtendido();
                naoAtendido.setItemPedidoId(item.getId());
                naoAtendido.setEquipamentoNome(item.getEquipamento().getNome());
                naoAtendido.setQuantidade(item.getQuantidade());
                if (maisProximo != null) {
                    naoAtendido.setMaiorDisponibilidadeEncontrada(disponibilidade.get(maisProximo.getId()).get(item.getId()));
                    naoAtendido.setDepositoComMaisDisponibilidadeId(maisProximo.getId());
                    naoAtendido.setDepositoComMaisDisponibilidadeNome(maisProximo.getNome());
                }
                naoAtendidos.add(naoAtendido);
            }
        }

        List<SugestaoAlocacaoResponse.GrupoDeposito> grupos = new ArrayList<>();
        for (Map.Entry<Long, List<ItemPedido>> entry : itensPorDeposito.entrySet()) {
            Deposito deposito = depositosAtivos.stream().filter(d -> d.getId().equals(entry.getKey())).findFirst().orElse(null);
            if (deposito == null) continue;
            SugestaoAlocacaoResponse.GrupoDeposito grupo = new SugestaoAlocacaoResponse.GrupoDeposito();
            grupo.setDepositoId(deposito.getId());
            grupo.setDepositoNome(deposito.getNome());
            grupo.setItens(entry.getValue().stream()
                    .map(i -> paraItemAlocado(i, disponibilidade.get(deposito.getId()).get(i.getId())))
                    .collect(Collectors.toList()));
            grupos.add(grupo);
        }

        resposta.setAtendeUmDeposito(false);
        resposta.setGrupos(grupos);
        resposta.setItensNaoAtendidos(naoAtendidos);
        return resposta;
    }

    private SugestaoAlocacaoResponse.ItemAlocado paraItemAlocado(ItemPedido item, long disponivel) {
        SugestaoAlocacaoResponse.ItemAlocado alocado = new SugestaoAlocacaoResponse.ItemAlocado();
        alocado.setItemPedidoId(item.getId());
        alocado.setEquipamentoId(item.getEquipamento().getId());
        alocado.setEquipamentoNome(item.getEquipamento().getNome());
        alocado.setQuantidade(item.getQuantidade());
        alocado.setDisponivelNoDeposito(disponivel);
        return alocado;
    }

    // ======================================================================
    // FLUXO DE DECISÃO
    // ======================================================================

    // Consultor confirma o pedido: precisa dizer de qual depósito cada item
    // vai sair (normalmente aceitando a sugestão do sistema, ver
    // sugerirAlocacaoDepositos). Revalida a disponibilidade real na hora —
    // outro pedido pode ter consumido o estoque entre a sugestão e a
    // confirmação — e grava o depósito escolhido em cada ItemPedido.
    @Transactional
    public PedidoResponse confirmar(Long id, ConfirmarPedidoRequest request) {
        Pedido pedido = buscarEntidade(id);
        if (pedido.getStatus() != StatusPedido.SOLICITADO) {
            throw new BusinessException("Só é possível confirmar pedidos com status SOLICITADO.");
        }

        List<ItemPedido> itens = itemRepository.findByPedidoId(pedido.getId());
        List<AlocacaoItemRequest> alocacoes = (request != null && request.getAlocacoes() != null)
                ? request.getAlocacoes() : List.of();

        if (alocacoes.size() != itens.size()) {
            throw new BusinessException("Informe o depósito de todos os itens do pedido antes de confirmar.");
        }

        Map<Long, Long> depositoPorItem = new HashMap<>();
        for (AlocacaoItemRequest a : alocacoes) {
            if (a.getItemPedidoId() == null || a.getDepositoId() == null) {
                throw new BusinessException("Alocação inválida: item e depósito são obrigatórios.");
            }
            depositoPorItem.put(a.getItemPedidoId(), a.getDepositoId());
        }

        Set<Long> idsDosItens = itens.stream().map(ItemPedido::getId).collect(Collectors.toSet());
        if (!depositoPorItem.keySet().equals(idsDosItens)) {
            throw new BusinessException("A lista de alocações não cobre exatamente os itens desse pedido.");
        }

        // Revalida disponibilidade em tempo real antes de gravar — a
        // sugestão pode ter ficado desatualizada.
        for (ItemPedido item : itens) {
            Long depositoId = depositoPorItem.get(item.getId());
            Deposito deposito = depositoRepository.findById(depositoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado: " + depositoId));
            long disponivel = unidadeRepository.countByEquipamentoIdAndStatusAndDepositoId(
                    item.getEquipamento().getId(), StatusUnidade.DISPONIVEL, depositoId);
            if (disponivel < item.getQuantidade()) {
                throw new BusinessException("Depósito " + deposito.getNome() + " não tem mais "
                        + item.getQuantidade() + " unidade(s) disponível(is) de " + item.getEquipamento().getNome()
                        + " (tem " + disponivel + "). Escolha outro depósito pra esse item.");
            }
            item.setDeposito(deposito);
            itemRepository.save(item);
        }

        // Frete final: recalculado a partir do(s) depósito(s) real(is) de
        // origem — a estimativa da solicitação (origem genérica) é substituída
        // aqui. Pedido dividido entre depósitos soma o trecho de cada item.
        // RETIRADA não tem frete — valorFrete permanece zero.
        if (pedido.getTipoEntrega() == TipoEntrega.RETIRADA) {
            pedido.setValorFrete(BigDecimal.ZERO);
        } else {
            BigDecimal freteFinal = BigDecimal.ZERO;
            Endereco destino = pedido.getEnderecoEntrega();
            for (ItemPedido item : itens) {
                Equipamento eq = item.getEquipamento();
                BigDecimal volume = freteService.volumeM3(
                        eq.getComprimentoCm(), eq.getLarguraCm(), eq.getAlturaCm(), item.getQuantidade());
                BigDecimal pesoItem = freteService.pesoConsideradoKg(eq.getPesoKg(), volume);
                int km = freteService.estimarDistanciaKm(
                        item.getDeposito() != null ? item.getDeposito().getEndereco() : null, destino);
                BigDecimal valorItem = item.getValorDiariaSnapshot()
                        .multiply(BigDecimal.valueOf(item.getQuantidade()))
                        .multiply(BigDecimal.valueOf(Math.max(1, ChronoUnit.DAYS.between(
                                pedido.getDataInicio(), pedido.getDataFim()))));
                freteFinal = freteFinal.add(freteService.calcularFrete(pesoItem, km, valorItem, false));
            }
            pedido.setValorFrete(freteFinal.setScale(2, RoundingMode.HALF_UP));
        }

        pedido.setConsultor(resolverFuncionarioLogado());
        pedido.setObservacoesConsultor(request != null ? request.getObservacoes() : null);
        // Confirmação do consultor aprova o pedido — fica pronto pra virar
        // expedição (próxima etapa do sistema).
        pedido.setStatus(StatusPedido.APROVADO);
        pedido.setConfirmadoEm(LocalDateTime.now());
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // Consultor recusa o pedido (ex.: sem estoque, fora da área de entrega).
    @Transactional
    public PedidoResponse recusar(Long id, PedidoDecisaoRequest request) {
        Pedido pedido = buscarEntidade(id);
        if (pedido.getStatus() != StatusPedido.SOLICITADO) {
            throw new BusinessException("Só é possível recusar pedidos com status SOLICITADO.");
        }
        if (request == null || request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new BusinessException("Informe o motivo da recusa.");
        }
        pedido.setConsultor(resolverFuncionarioLogado());
        pedido.setMotivoRecusa(request.getMotivo());
        pedido.setStatus(StatusPedido.RECUSADO);
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // Cliente (ou staff) cancela — só antes da aprovação (confirmação), pra
    // não cancelar algo que já pode estar virando expedição.
    @Transactional
    public PedidoResponse cancelar(Long id) {
        Pedido pedido = buscarEntidade(id);
        if (pedido.getStatus() == StatusPedido.APROVADO
                || pedido.getStatus() == StatusPedido.CANCELADO
                || pedido.getStatus() == StatusPedido.RECUSADO) {
            throw new BusinessException("Esse pedido não pode mais ser cancelado.");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceladoEm(LocalDateTime.now());
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // ======================================================================
    // AUXILIARES
    // ======================================================================

    // Resolve o endereço de entrega do pedido: se o cliente escolheu um
    // endereço já salvo (enderecoId), cria uma cópia avulsa; senão, persiste a
    // linha digitada na hora (enderecoEntrega). Ambas viram uma linha própria
    // na tabela enderecos referenciada por FK — pra não mudar retroativamente
    // pedidos já feitos quando o cliente editar/apagar o endereço salvo.
    private Endereco resolverEnderecoEntrega(Cliente cliente, PedidoRequest request) {
        if (request.getEnderecoId() != null) {
            Endereco endereco = enderecoRepository.findById(request.getEnderecoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado: " + request.getEnderecoId()));
            if (endereco.getCliente() == null || !endereco.getCliente().getId().equals(cliente.getId())) {
                throw new BusinessException("Esse endereço não pertence a esse cliente.");
            }
            return enderecoService.copiarAvulso(endereco);
        }

        var enderecoDigitado = request.getEnderecoEntrega();
        if (enderecoDigitado == null || enderecoDigitado.getRua() == null || enderecoDigitado.getRua().isBlank()) {
            throw new BusinessException("Escolha um endereço salvo ou informe um endereço de entrega.");
        }
        return enderecoService.persistirAvulso(enderecoDigitado);
    }

    private Pedido buscarEntidade(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
    }

    // Mesma lógica de sempre: nunca tocar em pedido.getItens() (orphanRemoval),
    // monta a resposta buscando os itens direto do repositório.
    private PedidoResponse construirResponse(Pedido pedido) {
        List<ItemPedido> itens = itemRepository.findByPedidoId(pedido.getId());
        return PedidoResponse.from(pedido, itens);
    }

    private String gerarCodigo() {
        String prefix = "PED-";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String base = prefix + datePart + "-";
        long count = pedidoRepository.count();
        return base + String.format("%04d", count + 1);
    }

    private String emailLogado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException("Usuário não autenticado.");
        }
        return auth.getPrincipal().toString();
    }

    private Cliente resolverClienteLogado() {
        Usuario usuario = usuarioRepository.findByEmail(emailLogado())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        if (usuario.getIdCliente() == null) {
            throw new BusinessException("Esse usuário não está vinculado a um cadastro de cliente.");
        }
        return clienteRepository.findById(usuario.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
    }

    private Funcionario resolverFuncionarioLogado() {
        Usuario usuario = usuarioRepository.findByEmail(emailLogado())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        if (usuario.getIdFuncionario() == null) {
            throw new BusinessException("Esse usuário não está vinculado a um cadastro de funcionário.");
        }
        return funcionarioRepository.findById(usuario.getIdFuncionario())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado."));
    }

    // Depósito fixo do funcionário logado, se houver — usado pra filtrar a
    // fila do conferente. Retorna null pra quem não está vinculado a um
    // funcionário (ex.: login ADMIN) ou cujo funcionário não tem depósito
    // fixo (ex.: GERENTE_OPERACOES, que enxerga tudo).
    private Long depositoDoFuncionarioLogadoOuNull() {
        try {
            Funcionario funcionario = resolverFuncionarioLogado();
            return funcionario.getDeposito() != null ? funcionario.getDeposito().getId() : null;
        } catch (BusinessException e) {
            return null;
        }
    }

    // Agrupa os itens de um pedido APROVADO por depósito e descarta os
    // grupos que já têm uma expedição ativa (não CANCELADO) gerada — o que
    // sobra são os grupos ainda pendentes de expedição.
    private List<PedidoResponse.GrupoPendenteResponse> montarGruposPendentes(Pedido pedido, List<ItemPedido> itens) {
        Map<Long, List<ItemPedido>> porDeposito = new LinkedHashMap<>();
        for (ItemPedido item : itens) {
            if (item.getDeposito() == null) continue; // pedido ainda não confirmado direito — não deveria acontecer em APROVADO
            porDeposito.computeIfAbsent(item.getDeposito().getId(), k -> new ArrayList<>()).add(item);
        }

        List<PedidoResponse.GrupoPendenteResponse> grupos = new ArrayList<>();
        for (Map.Entry<Long, List<ItemPedido>> entry : porDeposito.entrySet()) {
            boolean jaTemExpedicao = expedicaoRepository.existsByPedidoIdAndDepositoOrigemIdAndStatusNot(
                    pedido.getId(), entry.getKey(), StatusExpedicao.CANCELADO);
            if (jaTemExpedicao) continue;

            String depositoNome = entry.getValue().get(0).getDeposito().getNome();
            List<ItemPedidoResponse> itensResponse = entry.getValue().stream()
                    .map(ItemPedidoResponse::from)
                    .collect(Collectors.toList());
            grupos.add(new PedidoResponse.GrupoPendenteResponse(entry.getKey(), depositoNome, itensResponse));
        }
        return grupos;
    }
}
