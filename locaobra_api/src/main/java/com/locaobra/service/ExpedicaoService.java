package com.locaobra.service;

import com.locaobra.dto.request.ExpedicaoRequest;
import com.locaobra.dto.request.EnderecoRequest;
import com.locaobra.dto.request.ItemExpedicaoRequest;
import com.locaobra.dto.response.ExpedicaoRastreioResponse;
import com.locaobra.dto.response.ExpedicaoResponse;
import com.locaobra.dto.response.VistoriaResponse;
import com.locaobra.entity.*;
import com.locaobra.enums.StatusExpedicao;
import com.locaobra.enums.StatusPedido;
import com.locaobra.enums.StatusUnidade;
import com.locaobra.enums.TipoExpedicao;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpedicaoService {

    private final ExpedicaoRepository expedicaoRepository;
    private final ItemExpedicaoRepository itemRepository;
    private final VistoriaRepository vistoriaRepository;
    private final FotoVistoriaRepository fotoVistoriaRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UnidadeEquipamentoRepository unidadeRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final DepositoRepository depositoRepository;
    private final EnderecoService enderecoService;

    public ExpedicaoService(
            ExpedicaoRepository expedicaoRepository,
            ItemExpedicaoRepository itemRepository,
            VistoriaRepository vistoriaRepository,
            FotoVistoriaRepository fotoVistoriaRepository,
            ClienteRepository clienteRepository,
            FuncionarioRepository funcionarioRepository,
            UnidadeEquipamentoRepository unidadeRepository,
            EquipamentoRepository equipamentoRepository,
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository,
            ItemPedidoRepository itemPedidoRepository,
            DepositoRepository depositoRepository,
            EnderecoService enderecoService) {
        this.expedicaoRepository = expedicaoRepository;
        this.itemRepository = itemRepository;
        this.vistoriaRepository = vistoriaRepository;
        this.fotoVistoriaRepository = fotoVistoriaRepository;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.depositoRepository = depositoRepository;
        this.enderecoService = enderecoService;
    }

    @Transactional
    public ExpedicaoResponse criar(ExpedicaoRequest request) {
        if (request.getTipo() == null) {
            throw new BusinessException("Tipo de expedição é obrigatório (ENTREGA ou COLETA)");
        }
        if (request.getDataProgramada() == null) {
            throw new BusinessException("Data programada é obrigatória");
        }

        // COLETA sempre parte de uma ENTREGA concluída: cliente, endereço e
        // itens vêm de lá, não são digitados de novo. Resolve isso antes de
        // montar a expedição pra poder herdar os dados.
        Expedicao entregaOrigem = null;
        if (request.getTipo() == TipoExpedicao.COLETA) {
            if (request.getEntregaOrigemId() == null) {
                throw new BusinessException("Selecione a entrega que será coletada.");
            }
            entregaOrigem = expedicaoRepository.findById(request.getEntregaOrigemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada: " + request.getEntregaOrigemId()));
            if (entregaOrigem.getTipo() != TipoExpedicao.ENTREGA) {
                throw new BusinessException("A expedição de origem precisa ser do tipo ENTREGA.");
            }
            if (entregaOrigem.getStatus() != StatusExpedicao.ENTREGUE) {
                throw new BusinessException("Só é possível coletar uma entrega que já foi confirmada no local do cliente (passo 3).");
            }
            if (expedicaoRepository.existsByEntregaOrigemIdAndStatusNot(entregaOrigem.getId(), StatusExpedicao.CANCELADO)) {
                throw new BusinessException("Essa entrega já tem uma coleta em andamento ou concluída.");
            }
        }

        // Gerada pelo Conferente a partir de um pedido já aprovado (fila do
        // conferente): só faz sentido pra ENTREGA — COLETA é a etapa de volta,
        // que já deriva da própria expedição de entrega (entregaOrigem acima).
        Pedido pedido = null;
        Deposito depositoOrigem = null;
        if (request.getPedidoId() != null) {
            if (request.getTipo() != TipoExpedicao.ENTREGA) {
                throw new BusinessException("Expedição gerada a partir de um pedido só pode ser do tipo ENTREGA.");
            }
            pedido = pedidoRepository.findById(request.getPedidoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + request.getPedidoId()));
            if (pedido.getStatus() != StatusPedido.APROVADO) {
                throw new BusinessException("Só é possível gerar expedição a partir de um pedido com crédito APROVADO.");
            }
            if (request.getDepositoId() == null) {
                throw new BusinessException("Informe qual depósito está atendendo esse pedido.");
            }
            depositoOrigem = depositoRepository.findById(request.getDepositoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado: " + request.getDepositoId()));

            List<ItemPedido> itensDoGrupo = itemPedidoRepository.findByPedidoIdAndDepositoId(pedido.getId(), depositoOrigem.getId());
            if (itensDoGrupo.isEmpty()) {
                throw new BusinessException("Esse pedido não tem itens atribuídos ao depósito informado.");
            }
            // Trava por (pedido, depósito), não por pedido inteiro — permite uma
            // expedição por depósito quando o pedido foi desmembrado.
            if (expedicaoRepository.existsByPedidoIdAndDepositoOrigemIdAndStatusNot(
                    pedido.getId(), depositoOrigem.getId(), StatusExpedicao.CANCELADO)) {
                throw new BusinessException("Esse pedido já tem uma expedição em andamento ou concluída pra esse depósito.");
            }
        }

        // ---- Campos operacionais obrigatórios ----
        // Sem motorista, nenhum entregador enxerga a expedição (a lista é filtrada
        // por motorista); sem placa/autorizados, não há como conferir quem sai e
        // quem recebe. Por isso são exigidos aqui, e não só na tela.
        Funcionario motorista = resolverMotorista(request.getMotoristaId());
        if (request.getPlacaVeiculo() == null || request.getPlacaVeiculo().isBlank()) {
            throw new BusinessException("Informe a placa do veículo.");
        }
        if (request.getTipo() == TipoExpedicao.ENTREGA && nomesLimpos(request.getNomesAutorizados()).isEmpty()) {
            throw new BusinessException("Informe ao menos uma pessoa autorizada a receber o equipamento.");
        }

        // ---- Unidades da ENTREGA: reservadas com trava e conferidas contra o pedido ----
        Map<Long, UnidadeEquipamento> unidadesReservadas = new LinkedHashMap<>();
        if (request.getTipo() == TipoExpedicao.ENTREGA) {
            unidadesReservadas = reservarUnidades(request.getItens(), depositoOrigem);
            if (pedido != null) {
                validarItensContraPedido(pedido, depositoOrigem, unidadesReservadas.values());
            }
        }

        Expedicao expedicao = new Expedicao();
        expedicao.setCodigo(gerarCodigo());
        expedicao.setTipo(request.getTipo());
        expedicao.setStatus(StatusExpedicao.AGENDADO);
        expedicao.setDataProgramada(request.getDataProgramada());
        expedicao.setHorarioProgramado(request.getHorarioProgramado());
        expedicao.setPlacaVeiculo(request.getPlacaVeiculo());
        expedicao.setObservacoes(request.getObservacoes());
        expedicao.setEntregaOrigem(entregaOrigem);
        expedicao.setPedido(pedido);
        expedicao.setDepositoOrigem(depositoOrigem);

        if (pedido != null) {
            // Cliente e endereço vêm do pedido por padrão; um override explícito
            // no request (ex.: conferente ajustou o endereço na hora) prevalece.
            expedicao.setCliente(pedido.getCliente());
            expedicao.setEnderecoEntrega(
                    temEnderecoEntrega(request.getEnderecoEntrega())
                            ? enderecoService.persistirAvulso(request.getEnderecoEntrega()) : pedido.getEnderecoEntrega());
            // Pedido de RETIRADA (tipoEntrega = RETIRADA) não tem endereço de
            // entrega — o cliente busca no depósito. A expedição herda o
            // endereço do depósito de origem como referência do local de
            // busca/devolução, senão a validação de endereço abaixo quebraria.
            if (expedicao.getEnderecoEntrega() == null && depositoOrigem != null) {
                expedicao.setEnderecoEntrega(depositoOrigem.getEndereco());
            }
            aplicarNomesAutorizados(expedicao, request.getNomesAutorizados());
        } else if (entregaOrigem != null) {
            // Herdado da entrega de origem — endereço é o mesmo local onde o
            // equipamento foi deixado; cliente idem, a não ser que venha um
            // override explícito no request.
            expedicao.setEnderecoEntrega(
                    temEnderecoEntrega(request.getEnderecoEntrega())
                            ? enderecoService.persistirAvulso(request.getEnderecoEntrega()) : entregaOrigem.getEnderecoEntrega());
            expedicao.setCliente(entregaOrigem.getCliente());
            aplicarNomesAutorizados(expedicao,
                    request.getNomesAutorizados() != null && !request.getNomesAutorizados().isEmpty()
                            ? request.getNomesAutorizados()
                            : List.of(
                                    nvl(entregaOrigem.getNomeAutorizado1()),
                                    nvl(entregaOrigem.getNomeAutorizado2()),
                                    nvl(entregaOrigem.getNomeAutorizado3())));
        } else {
            expedicao.setEnderecoEntrega(enderecoService.persistirAvulso(request.getEnderecoEntrega()));
            // Entrega avulsa (sem pedido): o cliente é obrigatório — sem ele a
            // coleta herdaria "ninguém" e o equipamento ficaria sem responsável.
            if (request.getClienteId() == null) {
                throw new BusinessException("Selecione o cliente da entrega.");
            }
            expedicao.setCliente(clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.getClienteId())));
            aplicarNomesAutorizados(expedicao, request.getNomesAutorizados());
        }

        if (expedicao.getEnderecoEntrega() == null) {
            throw new BusinessException("Informe o endereço de entrega/coleta.");
        }
        expedicao.setMotorista(motorista);

        expedicao = expedicaoRepository.save(expedicao);

        if (entregaOrigem != null) {
            // Itens da coleta = mesmos itens que saíram na entrega de origem.
            for (ItemExpedicao itemOrigem : itemRepository.findByExpedicaoId(entregaOrigem.getId())) {
                ItemExpedicao item = new ItemExpedicao();
                item.setExpedicao(expedicao);
                item.setUnidade(itemOrigem.getUnidade());
                item.setEquipamento(itemOrigem.getEquipamento());
                item.setQuantidade(itemOrigem.getQuantidade());
                item.setObservacaoItem(itemOrigem.getObservacaoItem());
                itemRepository.save(item);
            }
        } else {
            // ENTREGA: cada item é UMA unidade física (patrimônio) já validada e
            // travada acima — quantidade é sempre 1.
            for (ItemExpedicaoRequest itemReq : request.getItens()) {
                UnidadeEquipamento unidade = unidadesReservadas.get(itemReq.getUnidadeId());

                ItemExpedicao item = new ItemExpedicao();
                item.setExpedicao(expedicao);
                item.setUnidade(unidade);
                item.setEquipamento(unidade.getEquipamento());
                item.setQuantidade(1);
                item.setObservacaoItem(itemReq.getObservacaoItem());
                itemRepository.save(item);

                // Unidade sai da vitrine assim que a entrega é agendada; se a
                // expedição for cancelada, liberarUnidades() devolve pra DISPONIVEL.
                unidade.setStatus(StatusUnidade.ALUGADO);
                unidadeRepository.save(unidade);
            }
        }

        return construirResponse(expedicao);
    }

    // ======================================================================
    // Validações da criação
    // ======================================================================

    private Funcionario resolverMotorista(Long motoristaId) {
        if (motoristaId == null) {
            throw new BusinessException("Informe o motorista (entregador) da expedição.");
        }
        Funcionario motorista = funcionarioRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + motoristaId));
        if (!Boolean.TRUE.equals(motorista.getStatus())) {
            throw new BusinessException("Funcionário está inativo: " + motorista.getNome());
        }
        if (motorista.getCargo() == null || !"ENTREGADOR".equals(motorista.getCargo().getNome())) {
            throw new BusinessException("O motorista precisa ter o cargo ENTREGADOR.");
        }
        return motorista;
    }

    private List<String> nomesLimpos(List<String> nomes) {
        return nomes == null ? List.of() : nomes.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .limit(3)
                .collect(Collectors.toList());
    }

    private String rotuloUnidade(UnidadeEquipamento unidade) {
        return unidade.getCodigoPatrimonio() != null ? unidade.getCodigoPatrimonio() : String.valueOf(unidade.getId());
    }

    // Carrega as unidades da ENTREGA com trava de escrita (SELECT ... FOR UPDATE),
    // em ordem crescente de id (evita deadlock entre duas expedições simultâneas),
    // e garante que todas estão DISPONIVEL e no depósito certo. Antes, só a tela
    // filtrava por DISPONIVEL: pela API dava pra reservar a mesma unidade duas vezes.
    private Map<Long, UnidadeEquipamento> reservarUnidades(List<ItemExpedicaoRequest> itens, Deposito depositoOrigem) {
        if (itens == null || itens.isEmpty()) {
            throw new BusinessException("Adicione pelo menos um item à expedição.");
        }
        List<Long> ids = new ArrayList<>();
        for (ItemExpedicaoRequest item : itens) {
            if (item.getUnidadeId() == null) {
                throw new BusinessException("Cada item da entrega precisa de uma unidade física (patrimônio).");
            }
            if (ids.contains(item.getUnidadeId())) {
                throw new BusinessException("A mesma unidade foi adicionada mais de uma vez.");
            }
            ids.add(item.getUnidadeId());
        }

        List<Long> ordenados = new ArrayList<>(ids);
        java.util.Collections.sort(ordenados);

        Map<Long, UnidadeEquipamento> travadas = new HashMap<>();
        for (Long unidadeId : ordenados) {
            UnidadeEquipamento unidade = unidadeRepository.findByIdParaReserva(unidadeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unidadeId));

            if (unidade.getStatus() != StatusUnidade.DISPONIVEL) {
                throw new BusinessException("A unidade " + rotuloUnidade(unidade)
                        + " não está disponível (situação atual: " + unidade.getStatus() + ").");
            }
            if (depositoOrigem != null) {
                Long unidadeDepositoId = unidade.getDeposito() != null ? unidade.getDeposito().getId() : null;
                if (!depositoOrigem.getId().equals(unidadeDepositoId)) {
                    throw new BusinessException("A unidade " + rotuloUnidade(unidade)
                            + " não pertence ao depósito " + depositoOrigem.getNome() + ".");
                }
            }
            travadas.put(unidadeId, unidade);
        }

        Map<Long, UnidadeEquipamento> naOrdemInformada = new LinkedHashMap<>();
        for (Long unidadeId : ids) {
            naOrdemInformada.put(unidadeId, travadas.get(unidadeId));
        }
        return naOrdemInformada;
    }

    // O que o conferente monta precisa bater com o que o cliente pediu (naquele
    // depósito): mesmo equipamento, mesma quantidade. Sem isso dava pra despachar
    // outro modelo, ou mais/menos unidades do que o pedido aprovado.
    private void validarItensContraPedido(Pedido pedido, Deposito deposito, Collection<UnidadeEquipamento> unidades) {
        Map<Long, Integer> esperado = new LinkedHashMap<>();
        Map<Long, String> nomes = new HashMap<>();
        for (ItemPedido itemPedido : itemPedidoRepository.findByPedidoIdAndDepositoId(pedido.getId(), deposito.getId())) {
            Long equipamentoId = itemPedido.getEquipamento().getId();
            esperado.merge(equipamentoId, itemPedido.getQuantidade(), Integer::sum);
            nomes.put(equipamentoId, itemPedido.getEquipamento().getNome());
        }

        Map<Long, Integer> atual = new HashMap<>();
        for (UnidadeEquipamento unidade : unidades) {
            Long equipamentoId = unidade.getEquipamento().getId();
            atual.merge(equipamentoId, 1, Integer::sum);
            nomes.putIfAbsent(equipamentoId, unidade.getEquipamento().getNome());
        }

        for (Long equipamentoId : atual.keySet()) {
            if (!esperado.containsKey(equipamentoId)) {
                throw new BusinessException("O equipamento \"" + nomes.get(equipamentoId)
                        + "\" não faz parte deste pedido (neste depósito).");
            }
        }
        for (Map.Entry<Long, Integer> e : esperado.entrySet()) {
            int qtdAtual = atual.getOrDefault(e.getKey(), 0);
            if (qtdAtual != e.getValue()) {
                throw new BusinessException("O pedido pede " + e.getValue() + " × " + nomes.get(e.getKey())
                        + " neste depósito, mas a expedição tem " + qtdAtual + ".");
            }
        }
    }

    // Entregas concluídas ainda sem coleta ativa vinculada — alimenta o select
    // de "qual entrega vou buscar" na tela de nova expedição do tipo COLETA.
    @Transactional(readOnly = true)
    public List<ExpedicaoResponse> listarEntregasParaColeta() {
        return expedicaoRepository.findEntregasConcluidasSemColetaAtiva()
                .stream()
                .map(this::construirResponse)
                .collect(Collectors.toList());
    }

    // Grava até 3 nomes de quem pode receber o equipamento — o resto da lista
    // (se vier vazio/em branco/repetido a mais) é simplesmente ignorado.
    private void aplicarNomesAutorizados(Expedicao expedicao, List<String> nomes) {
        List<String> limpos = nomes == null ? List.of() : nomes.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .limit(3)
                .collect(Collectors.toList());
        expedicao.setNomeAutorizado1(limpos.size() > 0 ? limpos.get(0) : null);
        expedicao.setNomeAutorizado2(limpos.size() > 1 ? limpos.get(1) : null);
        expedicao.setNomeAutorizado3(limpos.size() > 2 ? limpos.get(2) : null);
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private boolean temEnderecoEntrega(EnderecoRequest r) {
        return r != null && r.getRua() != null && !r.getRua().isBlank();
    }

    @Transactional(readOnly = true)
    public List<ExpedicaoResponse> listarTodos() {
        return filtrarPorMotorista(expedicaoRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ExpedicaoResponse> listarPorData(LocalDate data) {
        return filtrarPorMotorista(expedicaoRepository.findByDataProgramada(data));
    }

    @Transactional(readOnly = true)
    public List<ExpedicaoResponse> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return filtrarPorMotorista(expedicaoRepository.findByDataProgramadaBetween(inicio, fim));
    }

    @Transactional(readOnly = true)
    public List<ExpedicaoResponse> listarPorStatus(StatusExpedicao status) {
        return filtrarPorMotorista(expedicaoRepository.findByStatus(status));
    }

    @Transactional(readOnly = true)
    public ExpedicaoResponse buscarPorId(Long id) {
        Expedicao expedicao = findOrThrow(id);
        validarAcessoMotorista(expedicao);
        return construirResponse(expedicao);
    }

    // ======================================================================
    // RASTREIO DO CLIENTE (app mobile)
    // ----------------------------------------------------------------------
    // Mesmo princípio do "Meus Pedidos" (PedidoService.listarMeus): o cliente
    // só enxerga as próprias expedições, nunca as de outro cliente — mesmo
    // sabendo (ou adivinhando) o id. Por isso usamos ExpedicaoRastreioResponse
    // (sem dados internos de operação) e tratamos "não é minha" como 404, não
    // como 403: não confirmamos nem a existência da expedição de outro cliente.
    // ======================================================================

    @Transactional(readOnly = true)
    public List<ExpedicaoRastreioResponse> listarRastreioDoCliente() {
        Cliente cliente = resolverClienteLogado();
        return expedicaoRepository.findByClienteId(cliente.getId()).stream()
                .sorted(Comparator.comparing(Expedicao::getCriadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::construirRastreioResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpedicaoRastreioResponse buscarRastreioDoCliente(Long id) {
        Cliente cliente = resolverClienteLogado();
        Expedicao expedicao = findOrThrow(id);
        if (expedicao.getCliente() == null || !expedicao.getCliente().getId().equals(cliente.getId())) {
            throw new ResourceNotFoundException("Expedição não encontrada com ID: " + id);
        }
        return construirRastreioResponse(expedicao);
    }

    // Usado a partir de "Meus Pedidos": um pedido aprovado pode gerar mais de
    // uma expedição (itens espalhados em depósitos diferentes — ver criar()),
    // por isso devolve uma lista, não um único item.
    @Transactional(readOnly = true)
    public List<ExpedicaoRastreioResponse> listarRastreioPorPedido(Long pedidoId) {
        Cliente cliente = resolverClienteLogado();
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + pedidoId));
        if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(cliente.getId())) {
            throw new ResourceNotFoundException("Pedido não encontrado: " + pedidoId);
        }
        return expedicaoRepository.findByPedidoId(pedidoId).stream()
                .sorted(Comparator.comparing(Expedicao::getCriadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::construirRastreioResponse)
                .collect(Collectors.toList());
    }

    private ExpedicaoRastreioResponse construirRastreioResponse(Expedicao expedicao) {
        List<ItemExpedicao> itens = itemRepository.findByExpedicaoId(expedicao.getId());
        return ExpedicaoRastreioResponse.from(expedicao, itens);
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

    // ======================================================================
    // Filtro de visibilidade por motorista
    // ----------------------------------------------------------------------
    // O motorista de uma expedição é um funcionário com cargo ENTREGADOR
    // (é quem o campo "Motorista" da tela de nova expedição seleciona). A
    // regra de negócio é: o motorista SÓ enxerga as expedições em que ele é
    // o motorista designado. Os demais cargos (ADMIN, GERENTE_OPERACOES,
    // CONFERENTE, etc.) continuam vendo a lista completa.
    // ======================================================================

    // Retorna o id do funcionário logado apenas se ele for motorista
    // (cargo ENTREGADOR). Para qualquer outro cargo retorna null, o que
    // significa "sem filtro" — o usuário vê todas as expedições.
    private Long getIdFuncionarioSeMotorista() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        String email = auth.getPrincipal().toString();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null || usuario.getIdFuncionario() == null) {
            return null;
        }
        Funcionario funcionario = funcionarioRepository.findById(usuario.getIdFuncionario()).orElse(null);
        if (funcionario == null || funcionario.getCargo() == null || !"ENTREGADOR".equals(funcionario.getCargo().getNome())) {
            return null;
        }
        return usuario.getIdFuncionario();
    }

    private List<ExpedicaoResponse> filtrarPorMotorista(List<Expedicao> lista) {
        Long motoristaId = getIdFuncionarioSeMotorista();
        return lista.stream()
                .filter(e -> motoristaId == null
                        || (e.getMotorista() != null && motoristaId.equals(e.getMotorista().getId())))
                .map(this::construirResponse)
                .collect(Collectors.toList());
    }

    // Impede que um motorista abra o detalhe de uma expedição da qual não é
    // o motorista designado (mesma regra aplicada na listagem).
    private void validarAcessoMotorista(Expedicao expedicao) {
        Long motoristaId = getIdFuncionarioSeMotorista();
        if (motoristaId != null
                && (expedicao.getMotorista() == null || !motoristaId.equals(expedicao.getMotorista().getId()))) {
            throw new BusinessException("Você só pode acessar as expedições em que é o motorista designado.");
        }
    }

    // Máquina de estados aplicada NO BACK (antes só a tela impedia pular etapas):
    //   AGENDADO  -> EM_TRANSITO : check-out (conferente) — exige conferência dos itens
    //                               e, na ENTREGA, uma vistoria aprovada por unidade
    //   EM_TRANSITO -> ENTREGUE  : só por confirmarEntrega() (assinatura + documento + foto)
    //   ENTREGUE  -> CONCLUIDO   : check-in (conferente), só COLETA
    //   AGENDADO/EM_TRANSITO -> CANCELADO : cancelar()
    // Qualquer outra transição é recusada.
    @Transactional
    public ExpedicaoResponse atualizarStatus(Long id, StatusExpedicao status, List<Long> itensConferidos) {
        Expedicao expedicao = findOrThrow(id);

        if (status == StatusExpedicao.CANCELADO) {
            return cancelarInterno(expedicao, null);
        }
        if (status == StatusExpedicao.EM_TRANSITO) {
            iniciarCheckout(expedicao, itensConferidos);
        } else if (status == StatusExpedicao.CONCLUIDO) {
            concluirCheckin(expedicao);
        } else {
            throw new BusinessException("O status " + status + " não pode ser definido manualmente. "
                    + "A entrega é confirmada pelo entregador no local (assinatura, documento e foto).");
        }

        expedicao = expedicaoRepository.save(expedicao);
        return construirResponse(expedicao);
    }

    private void iniciarCheckout(Expedicao expedicao, List<Long> itensConferidos) {
        exigirCargo("CONFERENTE", "Somente o conferente pode registrar a saída (check-out) desta expedição.");

        if (expedicao.getStatus() != StatusExpedicao.AGENDADO) {
            throw new BusinessException("O check-out só pode ser feito em uma expedição Agendada.");
        }

        List<ItemExpedicao> itens = itemRepository.findByExpedicaoId(expedicao.getId());
        if (itens.isEmpty()) {
            throw new BusinessException("A expedição não tem itens.");
        }

        // Conferência: o conferente marca cada item antes de liberar a saída.
        Set<Long> esperados = itens.stream().map(ItemExpedicao::getId).collect(Collectors.toSet());
        Set<Long> conferidos = new HashSet<>(itensConferidos == null ? List.<Long>of() : itensConferidos);
        if (!conferidos.containsAll(esperados)) {
            throw new BusinessException("Confira todos os itens da expedição antes de liberar a saída.");
        }

        if (expedicao.getTipo() == TipoExpedicao.ENTREGA) {
            validarVistoriasDeSaida(expedicao, itens);
        }

        expedicao.setStatus(StatusExpedicao.EM_TRANSITO);
        expedicao.setCheckoutEm(LocalDateTime.now());
    }

    // Cada unidade da ENTREGA precisa ter sua vistoria de pré-saída, e nenhuma
    // pode ter sido reprovada (condição RUIM). Antes bastava UMA vistoria
    // qualquer pra liberar a expedição inteira — e só a tela conferia.
    private void validarVistoriasDeSaida(Expedicao expedicao, List<ItemExpedicao> itens) {
        List<String> semVistoria = new ArrayList<>();
        List<String> reprovadas = new ArrayList<>();

        for (ItemExpedicao item : itens) {
            UnidadeEquipamento unidade = item.getUnidade();
            if (unidade == null) continue;

            Vistoria vistoria = vistoriaRepository.findByExpedicaoIdAndUnidadeId(expedicao.getId(), unidade.getId())
                    .stream()
                    .filter(v -> v.getTipo() == com.locaobra.enums.TipoVistoria.ENTREGA)
                    .findFirst()
                    .orElse(null);

            if (vistoria == null) {
                semVistoria.add(rotuloUnidade(unidade));
            } else if ("RUIM".equals(vistoria.getCondicaoGeral())) {
                reprovadas.add(rotuloUnidade(unidade));
            }
        }

        if (!semVistoria.isEmpty()) {
            throw new BusinessException("Falta a vistoria de entrega da(s) unidade(s): " + String.join(", ", semVistoria) + ".");
        }
        if (!reprovadas.isEmpty()) {
            throw new BusinessException("Unidade(s) reprovada(s) na vistoria (condição RUIM): " + String.join(", ", reprovadas)
                    + ". Cancele a expedição para encaminhar a unidade à manutenção e crie outra com um equipamento em bom estado.");
        }
    }

    private void concluirCheckin(Expedicao expedicao) {
        // ENTREGA termina no passo 3 (confirmação no local, status ENTREGUE): o
        // equipamento fica com o cliente, não existe check-in pra ela. Só a COLETA
        // (que traz o equipamento de volta) passa por CONCLUIDO.
        if (expedicao.getTipo() == TipoExpedicao.ENTREGA) {
            throw new BusinessException("Uma expedição de ENTREGA termina na confirmação de entrega (passo 3); não há check-in aqui. Quando for buscar o equipamento, crie uma expedição do tipo COLETA.");
        }
        exigirCargo("CONFERENTE", "Somente o conferente pode registrar a entrada (check-in) desta expedição.");

        if (expedicao.getStatus() != StatusExpedicao.ENTREGUE || expedicao.getEntregaConfirmadaEm() == null) {
            throw new BusinessException("Confirme a coleta no local (assinatura, documento e foto do entregador) antes do check-in.");
        }

        expedicao.setStatus(StatusExpedicao.CONCLUIDO);
        expedicao.setCheckinEm(LocalDateTime.now());

        // O check-in da COLETA é o que efetivamente traz o equipamento de volta
        // pro depósito: só aí a unidade sai de ALUGADO e fica AGUARDANDO_MANUTENCAO
        // pro Conferente decidir o destino final.
        for (ItemExpedicao item : itemRepository.findByExpedicaoId(expedicao.getId())) {
            if (item.getUnidade() != null && item.getUnidade().getStatus() == StatusUnidade.ALUGADO) {
                item.getUnidade().setStatus(StatusUnidade.AGUARDANDO_MANUTENCAO);
                unidadeRepository.save(item.getUnidade());
            }
        }
    }

    // Passo intermediário entre o check-out e o check-in: o ENTREGADOR confirma,
    // no local do cliente, que a entrega (ou coleta) foi feita. Prova exigida:
    // nome + documento de quem assinou, a assinatura desenhada e a foto. A
    // data/hora é sempre a do servidor no momento do clique.
    @Transactional
    public ExpedicaoResponse confirmarEntrega(Long id, String assinaturaNome, String documento,
                                              String assinaturaImagemUrl, String fotoUrl, String observacao) {
        Expedicao expedicao = findOrThrow(id);
        exigirCargo("ENTREGADOR", "Somente o entregador pode confirmar a entrega no local do cliente.");
        // O entregador só mexe nas expedições em que é o motorista designado
        // (a mesma regra que já valia pra listar e abrir o detalhe).
        validarAcessoMotorista(expedicao);

        if (expedicao.getStatus() != StatusExpedicao.EM_TRANSITO) {
            throw new BusinessException("A entrega só pode ser confirmada depois do check-out (saída do depósito).");
        }
        if (expedicao.getEntregaConfirmadaEm() != null) {
            throw new BusinessException("A entrega já foi confirmada para esta expedição.");
        }
        if (assinaturaNome == null || assinaturaNome.isBlank()) {
            throw new BusinessException("Informe o nome de quem recebeu o equipamento.");
        }
        if (documento == null || documento.trim().length() < 5) {
            throw new BusinessException("Informe o documento (CPF ou RG) de quem assinou.");
        }
        if (assinaturaImagemUrl == null || assinaturaImagemUrl.isBlank()) {
            throw new BusinessException("A assinatura de quem recebeu é obrigatória.");
        }
        if (fotoUrl == null || fotoUrl.isBlank()) {
            throw new BusinessException("Foto da entrega no local é obrigatória.");
        }

        // Na ENTREGA, quem assina precisa estar na lista de autorizados definida
        // na criação (antes só a tela restringia; a API aceitava qualquer nome).
        String nome = assinaturaNome.trim();
        if (expedicao.getTipo() == TipoExpedicao.ENTREGA) {
            List<String> autorizados = java.util.stream.Stream.of(
                            expedicao.getNomeAutorizado1(), expedicao.getNomeAutorizado2(), expedicao.getNomeAutorizado3())
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.toList());
            boolean autorizado = autorizados.stream().anyMatch(n -> n.trim().equalsIgnoreCase(nome));
            if (!autorizados.isEmpty() && !autorizado) {
                throw new BusinessException("\"" + nome + "\" não está na lista de pessoas autorizadas a receber esta entrega.");
            }
        }

        expedicao.setAssinaturaEntrega(nome);
        expedicao.setDocumentoRecebedor(documento.trim());
        expedicao.setAssinaturaEntregaImagem(assinaturaImagemUrl);
        expedicao.setFotoEntrega(fotoUrl);
        expedicao.setObservacaoEntrega(limitar(observacao, 1000));
        expedicao.setEntregaConfirmadaEm(LocalDateTime.now());
        // A entrega foi feita no local: EM_TRANSITO -> ENTREGUE. Na COLETA, o check-in
        // (passo 3) depois transforma ENTREGUE em CONCLUIDO.
        expedicao.setStatus(StatusExpedicao.ENTREGUE);

        expedicao = expedicaoRepository.save(expedicao);
        return construirResponse(expedicao);
    }

    // O entregador chegou e não deu pra entregar/coletar (cliente ausente, recusou,
    // endereço não encontrado...). A expedição é encerrada como CANCELADO com o
    // motivo registrado; na ENTREGA as unidades voltam pro estoque e o pedido volta
    // pra fila do conferente, que gera uma nova expedição. Antes o único caminho era
    // o conferente cancelar (e as unidades ficavam presas em ALUGADO).
    @Transactional
    public ExpedicaoResponse registrarNaoRealizada(Long id, String motivo) {
        Expedicao expedicao = findOrThrow(id);
        exigirCargo("ENTREGADOR", "Somente o entregador pode registrar que a entrega/coleta não foi realizada.");
        validarAcessoMotorista(expedicao);

        if (expedicao.getStatus() != StatusExpedicao.EM_TRANSITO) {
            throw new BusinessException("Só é possível registrar \"não realizada\" com a expedição em trânsito.");
        }
        if (motivo == null || motivo.trim().length() < 5) {
            throw new BusinessException("Informe o motivo (ex.: cliente ausente, recusou receber, endereço não encontrado).");
        }

        expedicao.setStatus(StatusExpedicao.CANCELADO);
        expedicao.setMotivoCancelamento(limitar("Não realizada: " + motivo.trim(), 500));
        liberarUnidades(expedicao);

        expedicao = expedicaoRepository.save(expedicao);
        return construirResponse(expedicao);
    }

    private String limitar(String texto, int max) {
        if (texto == null || texto.isBlank()) return null;
        String limpo = texto.trim();
        return limpo.length() > max ? limpo.substring(0, max) : limpo;
    }

    // Confere se quem está logado tem o cargo certo pra fazer essa etapa.
    // ADMIN e GERENTE_OPERACOES sempre podem, por supervisionarem a operação.
    private void exigirCargo(String cargoEsperado, String mensagemErro) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean temPermissao = auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_GERENTE_OPERACOES") ||
                a.getAuthority().equals("ROLE_" + cargoEsperado));
        if (!temPermissao) {
            throw new BusinessException(mensagemErro);
        }
    }

    @Transactional
    public void cancelar(Long id, String motivo) {
        cancelarInterno(findOrThrow(id), motivo);
    }

    private ExpedicaoResponse cancelarInterno(Expedicao expedicao, String motivo) {
        StatusExpedicao atual = expedicao.getStatus();
        if (atual == StatusExpedicao.CANCELADO) {
            throw new BusinessException("Esta expedição já está cancelada.");
        }
        if (atual == StatusExpedicao.ENTREGUE || atual == StatusExpedicao.CONCLUIDO) {
            throw new BusinessException("Uma expedição já entregue/concluída não pode ser cancelada.");
        }
        // Depois do check-out o equipamento já está na estrada: só o gerente cancela.
        // O entregador tem o caminho próprio "não realizada".
        if (atual == StatusExpedicao.EM_TRANSITO) {
            exigirCargo("GERENTE_OPERACOES", "Só o gerente de operações pode cancelar uma expedição que já saiu do depósito.");
        }

        expedicao.setStatus(StatusExpedicao.CANCELADO);
        expedicao.setMotivoCancelamento(limitar(motivo, 500));
        liberarUnidades(expedicao);

        expedicao = expedicaoRepository.save(expedicao);
        return construirResponse(expedicao);
    }

    // Ao agendar uma ENTREGA as unidades viram ALUGADO (saem da vitrine). Se a
    // entrega não acontece, elas precisam voltar — senão ficam presas: o modal só
    // lista DISPONIVEL. Unidade reprovada na vistoria (RUIM) vai pra manutenção, não
    // pro estoque. COLETA não mexe: o equipamento continua com o cliente.
    private void liberarUnidades(Expedicao expedicao) {
        if (expedicao.getTipo() != TipoExpedicao.ENTREGA) return;

        for (ItemExpedicao item : itemRepository.findByExpedicaoId(expedicao.getId())) {
            UnidadeEquipamento unidade = item.getUnidade();
            if (unidade == null || unidade.getStatus() != StatusUnidade.ALUGADO) continue;

            boolean reprovada = vistoriaRepository.findByExpedicaoIdAndUnidadeId(expedicao.getId(), unidade.getId())
                    .stream()
                    .anyMatch(v -> v.getTipo() == com.locaobra.enums.TipoVistoria.ENTREGA && "RUIM".equals(v.getCondicaoGeral()));

            unidade.setStatus(reprovada ? StatusUnidade.AGUARDANDO_MANUTENCAO : StatusUnidade.DISPONIVEL);
            unidadeRepository.save(unidade);
        }
    }

    @Transactional
    public void deletar(Long id) {
        Expedicao expedicao = findOrThrow(id);
        if (expedicao.getStatus() == StatusExpedicao.AGENDADO || expedicao.getStatus() == StatusExpedicao.EM_TRANSITO) {
            liberarUnidades(expedicao);
        }
        expedicaoRepository.delete(expedicao);
    }

    private Expedicao findOrThrow(Long id) {
        return expedicaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expedição não encontrada com ID: " + id));
    }

    // IMPORTANTE: nunca chame expedicao.getItens().clear()/.addAll() aqui.
    // Expedicao.itens e Expedicao.vistorias são @OneToMany(orphanRemoval = true);
    // limpar essa coleção gerenciada pelo Hibernate agenda a EXCLUSÃO das linhas
    // no banco, mesmo que os mesmos itens sejam adicionados de volta em seguida.
    // Por isso montamos a resposta com listas buscadas direto do repositório,
    // sem tocar na coleção da entidade.
    private ExpedicaoResponse construirResponse(Expedicao expedicao) {
        List<ItemExpedicao> itens = itemRepository.findByExpedicaoId(expedicao.getId());
        List<VistoriaResponse> vistorias = vistoriaRepository.findByExpedicaoId(expedicao.getId())
                .stream()
                .map(v -> VistoriaResponse.from(v, fotoVistoriaRepository.findByVistoriaId(v.getId())))
                .collect(Collectors.toList());
        return ExpedicaoResponse.from(expedicao, itens, vistorias);
    }

    private String gerarCodigo() {
        String prefix = "EXP-";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String base = prefix + datePart + "-";
        long count = expedicaoRepository.count();
        return base + String.format("%04d", count + 1);
    }
}