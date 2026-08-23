package com.locaobra.service;

import com.locaobra.dto.request.ExpedicaoRequest;
import com.locaobra.dto.request.ItemExpedicaoRequest;
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
import java.util.List;
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
            DepositoRepository depositoRepository) {
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
                    request.getEnderecoEntrega() != null && !request.getEnderecoEntrega().isBlank()
                            ? request.getEnderecoEntrega() : pedido.getEnderecoEntrega());
            aplicarNomesAutorizados(expedicao, request.getNomesAutorizados());
        } else if (entregaOrigem != null) {
            // Herdado da entrega de origem — endereço é o mesmo local onde o
            // equipamento foi deixado; cliente idem, a não ser que venha um
            // override explícito no request.
            expedicao.setEnderecoEntrega(
                    request.getEnderecoEntrega() != null && !request.getEnderecoEntrega().isBlank()
                            ? request.getEnderecoEntrega() : entregaOrigem.getEnderecoEntrega());
            expedicao.setCliente(entregaOrigem.getCliente());
            aplicarNomesAutorizados(expedicao,
                    request.getNomesAutorizados() != null && !request.getNomesAutorizados().isEmpty()
                            ? request.getNomesAutorizados()
                            : List.of(
                                    nvl(entregaOrigem.getNomeAutorizado1()),
                                    nvl(entregaOrigem.getNomeAutorizado2()),
                                    nvl(entregaOrigem.getNomeAutorizado3())));
        } else {
            expedicao.setEnderecoEntrega(request.getEnderecoEntrega());
            if (request.getClienteId() != null) {
                expedicao.setCliente(clienteRepository.findById(request.getClienteId())
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.getClienteId())));
            }
            aplicarNomesAutorizados(expedicao, request.getNomesAutorizados());
        }

        if (request.getMotoristaId() != null) {
            Funcionario motorista = funcionarioRepository.findById(request.getMotoristaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + request.getMotoristaId()));
            if (!motorista.getStatus()) {
                throw new BusinessException("Funcionário está inativo: " + motorista.getNome());
            }
            expedicao.setMotorista(motorista);
        }

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
        } else if (request.getItens() != null && !request.getItens().isEmpty()) {
            for (ItemExpedicaoRequest itemReq : request.getItens()) {
                ItemExpedicao item = new ItemExpedicao();
                item.setExpedicao(expedicao);
                item.setQuantidade(itemReq.getQuantidade() != null ? itemReq.getQuantidade() : 1);
                item.setObservacaoItem(itemReq.getObservacaoItem());

                if (itemReq.getUnidadeId() != null) {
                    UnidadeEquipamento unidade = unidadeRepository.findById(itemReq.getUnidadeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + itemReq.getUnidadeId()));

                    // Quando a expedição vem de um pedido, cada unidade escolhida
                    // precisa realmente estar no depósito que essa expedição está
                    // cobrindo — evita o conferente misturar unidade de outro
                    // depósito por engano.
                    if (depositoOrigem != null) {
                        Long unidadeDepositoId = unidade.getDeposito() != null ? unidade.getDeposito().getId() : null;
                        if (!depositoOrigem.getId().equals(unidadeDepositoId)) {
                            throw new BusinessException("A unidade " + (unidade.getCodigoPatrimonio() != null ? unidade.getCodigoPatrimonio() : unidade.getId())
                                    + " não pertence ao depósito " + depositoOrigem.getNome() + ".");
                        }
                    }

                    item.setUnidade(unidade);
                    item.setEquipamento(unidade.getEquipamento());

                    // Unidade sai da vitrine quando é expedida (ENTREGA)
                    if (request.getTipo() == TipoExpedicao.ENTREGA && unidade.getStatus() == StatusUnidade.DISPONIVEL) {
                        unidade.setStatus(StatusUnidade.ALUGADO);
                        unidadeRepository.save(unidade);
                    }
                } else if (itemReq.getEquipamentoId() != null) {
                    Equipamento equipamento = equipamentoRepository.findById(itemReq.getEquipamentoId())
                            .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + itemReq.getEquipamentoId()));
                    item.setEquipamento(equipamento);
                } else {
                    throw new BusinessException("Cada item deve ter unidadeId ou equipamentoId");
                }

                itemRepository.save(item);
            }
        }

        return construirResponse(expedicao);
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

    @Transactional
    public ExpedicaoResponse atualizarStatus(Long id, StatusExpedicao status) {
        Expedicao expedicao = findOrThrow(id);

        // ENTREGA termina no passo 3 (confirmação de entrega no local), que já
        // deixa o status em ENTREGUE — não existe passo 4/check-in pra ela, porque
        // o equipamento fica com o cliente, não volta pro depósito. Só a COLETA
        // (que é quem efetivamente busca o equipamento de volta) passa por CONCLUIDO.
        if (status == StatusExpedicao.CONCLUIDO && expedicao.getTipo() == TipoExpedicao.ENTREGA) {
            throw new BusinessException("Uma expedição de ENTREGA termina na confirmação de entrega (passo 3); não há check-in aqui. Quando for buscar o equipamento, crie uma expedição do tipo COLETA.");
        }

        validarPermissaoTransicao(status);

        if (status == StatusExpedicao.CONCLUIDO && expedicao.getEntregaConfirmadaEm() == null) {
            throw new BusinessException("Confirme a entrega no local (assinatura + foto do entregador) antes do check-in.");
        }

        expedicao.setStatus(status);

        if (status == StatusExpedicao.EM_TRANSITO) {
            expedicao.setCheckoutEm(LocalDateTime.now());
        }
        if (status == StatusExpedicao.CONCLUIDO) {
            expedicao.setCheckinEm(LocalDateTime.now());

            // O check-in da COLETA é o que efetivamente traz o equipamento de volta
            // pro depósito: só aí a unidade sai de ALUGADO e fica AGUARDANDO_MANUTENCAO
            // pro Conferente decidir o destino final. O check-in da ENTREGA é só o
            // motorista/conferente confirmando que o veículo voltou — o equipamento
            // continua com o cliente, então a unidade permanece ALUGADO.
            if (expedicao.getTipo() == TipoExpedicao.COLETA) {
                for (ItemExpedicao item : itemRepository.findByExpedicaoId(id)) {
                    if (item.getUnidade() != null && item.getUnidade().getStatus() == StatusUnidade.ALUGADO) {
                        item.getUnidade().setStatus(StatusUnidade.AGUARDANDO_MANUTENCAO);
                        unidadeRepository.save(item.getUnidade());
                    }
                }
            }
        }

        expedicao = expedicaoRepository.save(expedicao);
        return construirResponse(expedicao);
    }

    @Transactional
    public void atualizarAssinatura(Long id, String assinatura) {
        Expedicao expedicao = findOrThrow(id);
        expedicao.setAssinaturaCliente(assinatura);
        expedicaoRepository.save(expedicao);
    }

    // Passo intermediário entre o check-out e o check-in: o ENTREGADOR confirma,
    // no local do cliente, que a entrega foi feita — com assinatura de quem
    // recebeu e foto do equipamento entregue. A data/hora é sempre a do
    // servidor no momento do clique, nunca informada pelo front.
    @Transactional
    public ExpedicaoResponse confirmarEntrega(Long id, String assinatura, String fotoUrl) {
        Expedicao expedicao = findOrThrow(id);
        exigirCargo("ENTREGADOR", "Somente o entregador pode confirmar a entrega no local do cliente.");

        if (expedicao.getStatus() != StatusExpedicao.EM_TRANSITO) {
            throw new BusinessException("A entrega só pode ser confirmada depois do check-out (saída do depósito).");
        }
        if (expedicao.getEntregaConfirmadaEm() != null) {
            throw new BusinessException("A entrega já foi confirmada para esta expedição.");
        }
        if (assinatura == null || assinatura.isBlank()) {
            throw new BusinessException("Assinatura de quem recebeu o equipamento é obrigatória.");
        }
        if (fotoUrl == null || fotoUrl.isBlank()) {
            throw new BusinessException("Foto da entrega no local é obrigatória.");
        }

        expedicao.setAssinaturaEntrega(assinatura);
        expedicao.setFotoEntrega(fotoUrl);
        expedicao.setEntregaConfirmadaEm(LocalDateTime.now());
        // A entrega foi feita no local: o status passa de EM_TRANSITO para ENTREGUE.
        // O check-in (passo 4) depois transforma ENTREGUE em CONCLUIDO.
        expedicao.setStatus(StatusExpedicao.ENTREGUE);

        expedicao = expedicaoRepository.save(expedicao);
        return construirResponse(expedicao);
    }

    // Confere se quem está logado tem o cargo certo pra fazer essa transição
    // de status. Regra de negócio: quem registra a SAÍDA (check-out) e a
    // ENTRADA (check-in) é o CONFERENTE.
    // ADMIN e GERENTE_OPERACOES sempre podem, por supervisionarem a operação.
    private void validarPermissaoTransicao(StatusExpedicao novoStatus) {
        if (novoStatus == StatusExpedicao.EM_TRANSITO) {
            exigirCargo("CONFERENTE", "Somente o conferente pode registrar a saída (check-out) desta expedição.");
        } else if (novoStatus == StatusExpedicao.CONCLUIDO) {
            exigirCargo("CONFERENTE", "Somente o conferente pode registrar a entrada (check-in) desta expedição.");
        }
        // CANCELADO fica liberado pra qualquer cargo com acesso à expedição
        // (já filtrado no SecurityConfig) — cancelar é uma exceção, não uma
        // etapa do fluxo normal.
    }

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
    public void cancelar(Long id) {
        Expedicao expedicao = findOrThrow(id);
        expedicao.setStatus(StatusExpedicao.CANCELADO);
        expedicaoRepository.save(expedicao);
    }

    @Transactional
    public void deletar(Long id) {
        Expedicao expedicao = findOrThrow(id);
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