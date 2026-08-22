package com.locaobra.service;

import com.locaobra.dto.request.ItemPedidoRequest;
import com.locaobra.dto.request.PedidoDecisaoRequest;
import com.locaobra.dto.request.PedidoRequest;
import com.locaobra.dto.response.PedidoResponse;
import com.locaobra.entity.*;
import com.locaobra.enums.StatusPedido;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ItemPedidoRepository itemRepository,
            ClienteRepository clienteRepository,
            EquipamentoRepository equipamentoRepository,
            FuncionarioRepository funcionarioRepository,
            UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.itemRepository = itemRepository;
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.usuarioRepository = usuarioRepository;
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
        if (request.getEnderecoEntrega() == null || request.getEnderecoEntrega().isBlank()) {
            throw new BusinessException("Endereço de entrega é obrigatório.");
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new BusinessException("Adicione ao menos um equipamento ao pedido.");
        }

        Pedido pedido = new Pedido();
        pedido.setCodigo(gerarCodigo());
        pedido.setStatus(StatusPedido.SOLICITADO);
        pedido.setCliente(cliente);
        pedido.setDataInicio(request.getDataInicio());
        pedido.setDataFim(request.getDataFim());
        pedido.setEnderecoEntrega(request.getEnderecoEntrega());
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
        pedido = pedidoRepository.save(pedido);

        return construirResponse(pedido);
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

    // Fila do analista de crédito: pedidos já confirmados pelo consultor.
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarFilaCredito() {
        return listarPorStatus(StatusPedido.CONFIRMADO);
    }

    // Fila do conferente: pedidos com crédito aprovado, prontos pra virar
    // expedição. Some da lista assim que uma expedição é gerada (ver
    // PedidoRepository.findAprovadosSemExpedicaoAtiva).
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarFilaConferente() {
        return pedidoRepository.findAprovadosSemExpedicaoAtiva().stream()
                .map(this::construirResponse)
                .collect(Collectors.toList());
    }

    // "Meus pedidos" do cliente logado.
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarMeus() {
        Cliente cliente = resolverClienteLogado();
        return pedidoRepository.findByClienteId(cliente.getId()).stream()
                .map(this::construirResponse)
                .collect(Collectors.toList());
    }

    // ======================================================================
    // FLUXO DE DECISÃO
    // ======================================================================

    // Consultor confirma o pedido como está e manda pra análise de crédito.
    @Transactional
    public PedidoResponse confirmar(Long id, PedidoDecisaoRequest request) {
        Pedido pedido = buscarEntidade(id);
        if (pedido.getStatus() != StatusPedido.SOLICITADO) {
            throw new BusinessException("Só é possível confirmar pedidos com status SOLICITADO.");
        }
        pedido.setConsultor(resolverFuncionarioLogado());
        pedido.setObservacoesConsultor(request != null ? request.getObservacoes() : null);
        pedido.setStatus(StatusPedido.CONFIRMADO);
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

    // Analista de credenciamento aprova o crédito — pedido fica pronto pra
    // virar expedição (próxima etapa do sistema).
    @Transactional
    public PedidoResponse aprovarCredito(Long id, PedidoDecisaoRequest request) {
        Pedido pedido = buscarEntidade(id);
        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new BusinessException("Só é possível aprovar crédito de pedidos com status CONFIRMADO.");
        }
        pedido.setAnalistaCredito(resolverFuncionarioLogado());
        pedido.setStatus(StatusPedido.APROVADO);
        pedido.setAnalisadoEm(LocalDateTime.now());
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    @Transactional
    public PedidoResponse reprovarCredito(Long id, PedidoDecisaoRequest request) {
        Pedido pedido = buscarEntidade(id);
        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new BusinessException("Só é possível reprovar crédito de pedidos com status CONFIRMADO.");
        }
        if (request == null || request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new BusinessException("Informe o motivo da reprovação.");
        }
        pedido.setAnalistaCredito(resolverFuncionarioLogado());
        pedido.setMotivoRecusa(request.getMotivo());
        pedido.setStatus(StatusPedido.REPROVADO);
        pedido.setAnalisadoEm(LocalDateTime.now());
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // Cliente (ou staff) cancela — só antes da aprovação de crédito, pra não
    // cancelar algo que já pode estar virando expedição.
    @Transactional
    public PedidoResponse cancelar(Long id) {
        Pedido pedido = buscarEntidade(id);
        if (pedido.getStatus() == StatusPedido.APROVADO
                || pedido.getStatus() == StatusPedido.CANCELADO
                || pedido.getStatus() == StatusPedido.RECUSADO
                || pedido.getStatus() == StatusPedido.REPROVADO) {
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
}
