package com.locaobra.dto.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Corpo do POST /api/pedidos/estimar-frete — usado pelo carrinho pra mostrar
// o frete ANTES de o cliente fechar o pedido. Espelha o PedidoRequest só nos
// campos que o cálculo precisa (endereço destino + itens).
public class EstimarFreteRequest {

    // Endereço já salvo do cliente (o service valida se é dele).
    private Long enderecoId;

    // Endereço digitado na hora, quando não há enderecoId.
    private EnderecoRequest enderecoEntrega;

    // Período da locação — necessário pro ad valorem (0,6% do valor total da
    // locação, que depende de quantos dias).
    private LocalDate dataInicio;
    private LocalDate dataFim;

    private List<ItemPedidoRequest> itens = new ArrayList<>();

    public Long getEnderecoId() { return enderecoId; }
    public void setEnderecoId(Long enderecoId) { this.enderecoId = enderecoId; }

    public EnderecoRequest getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(EnderecoRequest enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public List<ItemPedidoRequest> getItens() { return itens; }
    public void setItens(List<ItemPedidoRequest> itens) { this.itens = itens; }
}