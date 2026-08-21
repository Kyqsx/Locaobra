package com.locaobra.dto.response;

import com.locaobra.entity.ItemPedido;
import com.locaobra.entity.Pedido;
import com.locaobra.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class PedidoResponse {

    private Long id;
    private String codigo;
    private StatusPedido status;
    private Long clienteId;
    private String clienteNome;
    private Long consultorId;
    private String consultorNome;
    private Long analistaCreditoId;
    private String analistaCreditoNome;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Long diasLocacao;
    private String enderecoEntrega;
    private String observacoesCliente;
    private String observacoesConsultor;
    private String motivoRecusa;
    private BigDecimal valorTotalEstimado;
    private LocalDateTime confirmadoEm;
    private LocalDateTime analisadoEm;
    private LocalDateTime canceladoEm;
    private LocalDateTime criadoEm;
    private List<ItemPedidoResponse> itens;

    public static PedidoResponse from(Pedido p, List<ItemPedido> itens) {
        PedidoResponse r = new PedidoResponse();
        r.id = p.getId();
        r.codigo = p.getCodigo();
        r.status = p.getStatus();
        r.clienteId = p.getCliente() != null ? p.getCliente().getId() : null;
        r.clienteNome = p.getCliente() != null ? p.getCliente().getNome() : null;
        r.consultorId = p.getConsultor() != null ? p.getConsultor().getId() : null;
        r.consultorNome = p.getConsultor() != null ? p.getConsultor().getNome() : null;
        r.analistaCreditoId = p.getAnalistaCredito() != null ? p.getAnalistaCredito().getId() : null;
        r.analistaCreditoNome = p.getAnalistaCredito() != null ? p.getAnalistaCredito().getNome() : null;
        r.dataInicio = p.getDataInicio();
        r.dataFim = p.getDataFim();
        r.diasLocacao = (p.getDataInicio() != null && p.getDataFim() != null)
                ? Math.max(1, ChronoUnit.DAYS.between(p.getDataInicio(), p.getDataFim()))
                : null;
        r.enderecoEntrega = p.getEnderecoEntrega();
        r.observacoesCliente = p.getObservacoesCliente();
        r.observacoesConsultor = p.getObservacoesConsultor();
        r.motivoRecusa = p.getMotivoRecusa();
        r.valorTotalEstimado = p.getValorTotalEstimado();
        r.confirmadoEm = p.getConfirmadoEm();
        r.analisadoEm = p.getAnalisadoEm();
        r.canceladoEm = p.getCanceladoEm();
        r.criadoEm = p.getCriadoEm();
        r.itens = itens.stream().map(ItemPedidoResponse::from).collect(Collectors.toList());
        return r;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public StatusPedido getStatus() { return status; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNome() { return clienteNome; }
    public Long getConsultorId() { return consultorId; }
    public String getConsultorNome() { return consultorNome; }
    public Long getAnalistaCreditoId() { return analistaCreditoId; }
    public String getAnalistaCreditoNome() { return analistaCreditoNome; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public Long getDiasLocacao() { return diasLocacao; }
    public String getEnderecoEntrega() { return enderecoEntrega; }
    public String getObservacoesCliente() { return observacoesCliente; }
    public String getObservacoesConsultor() { return observacoesConsultor; }
    public String getMotivoRecusa() { return motivoRecusa; }
    public BigDecimal getValorTotalEstimado() { return valorTotalEstimado; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public LocalDateTime getAnalisadoEm() { return analisadoEm; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<ItemPedidoResponse> getItens() { return itens; }
}
