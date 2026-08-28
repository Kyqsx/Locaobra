package com.locaobra.dto.response;

import com.locaobra.entity.ItemPedido;
import com.locaobra.entity.Pedido;
import com.locaobra.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private EnderecoResponse enderecoEntrega;
    private String observacoesCliente;
    private String observacoesConsultor;
    private String motivoRecusa;
    private BigDecimal valorTotalEstimado;
    private LocalDateTime confirmadoEm;
    private LocalDateTime analisadoEm;
    private LocalDateTime canceladoEm;
    private LocalDateTime criadoEm;
    private List<ItemPedidoResponse> itens;
    // Nomes distintos dos depósitos já atribuídos aos itens (preenchido a
    // partir de CONFIRMADO em diante). Útil pra mostrar de cara, em qualquer
    // tela, se o pedido vai sair de um depósito só ou foi desmembrado.
    private List<String> depositosEnvolvidos;
    // Só preenchido na fila do conferente: quais depósitos ainda precisam de
    // uma expedição gerada (ver PedidoService.listarFilaConferente).
    private List<GrupoPendenteResponse> gruposPendentes = new ArrayList<>();

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
        r.enderecoEntrega = EnderecoResponse.from(p.getEnderecoEntrega());
        r.observacoesCliente = p.getObservacoesCliente();
        r.observacoesConsultor = p.getObservacoesConsultor();
        r.motivoRecusa = p.getMotivoRecusa();
        r.valorTotalEstimado = p.getValorTotalEstimado();
        r.confirmadoEm = p.getConfirmadoEm();
        r.analisadoEm = p.getAnalisadoEm();
        r.canceladoEm = p.getCanceladoEm();
        r.criadoEm = p.getCriadoEm();
        r.itens = itens.stream().map(ItemPedidoResponse::from).collect(Collectors.toList());
        r.depositosEnvolvidos = itens.stream()
                .filter(i -> i.getDeposito() != null)
                .map(i -> i.getDeposito().getNome())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().collect(Collectors.toList());
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
    public EnderecoResponse getEnderecoEntrega() { return enderecoEntrega; }
    public String getObservacoesCliente() { return observacoesCliente; }
    public String getObservacoesConsultor() { return observacoesConsultor; }
    public String getMotivoRecusa() { return motivoRecusa; }
    public BigDecimal getValorTotalEstimado() { return valorTotalEstimado; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public LocalDateTime getAnalisadoEm() { return analisadoEm; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<ItemPedidoResponse> getItens() { return itens; }
    public List<String> getDepositosEnvolvidos() { return depositosEnvolvidos; }
    public List<GrupoPendenteResponse> getGruposPendentes() { return gruposPendentes; }
    public void setGruposPendentes(List<GrupoPendenteResponse> gruposPendentes) { this.gruposPendentes = gruposPendentes; }

    // Um depósito do pedido que ainda não teve expedição gerada — a fila do
    // conferente mostra um botão "Gerar expedição" por grupo desses.
    public static class GrupoPendenteResponse {
        private Long depositoId;
        private String depositoNome;
        private List<ItemPedidoResponse> itens;

        public GrupoPendenteResponse(Long depositoId, String depositoNome, List<ItemPedidoResponse> itens) {
            this.depositoId = depositoId;
            this.depositoNome = depositoNome;
            this.itens = itens;
        }

        public Long getDepositoId() { return depositoId; }
        public String getDepositoNome() { return depositoNome; }
        public List<ItemPedidoResponse> getItens() { return itens; }
    }
}
