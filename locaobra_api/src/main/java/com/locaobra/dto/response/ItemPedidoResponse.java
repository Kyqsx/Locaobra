package com.locaobra.dto.response;

import com.locaobra.entity.ItemPedido;

import java.math.BigDecimal;

public class ItemPedidoResponse {

    private Long id;
    private Long equipamentoId;
    private String equipamentoNome;
    private String equipamentoCategoria;
    private Integer quantidade;
    private BigDecimal valorDiariaSnapshot;
    private String observacaoItem;

    public static ItemPedidoResponse from(ItemPedido item) {
        ItemPedidoResponse r = new ItemPedidoResponse();
        r.id = item.getId();
        r.equipamentoId = item.getEquipamento() != null ? item.getEquipamento().getId() : null;
        r.equipamentoNome = item.getEquipamento() != null ? item.getEquipamento().getNome() : null;
        r.equipamentoCategoria = item.getEquipamento() != null ? item.getEquipamento().getCategoria() : null;
        r.quantidade = item.getQuantidade();
        r.valorDiariaSnapshot = item.getValorDiariaSnapshot();
        r.observacaoItem = item.getObservacaoItem();
        return r;
    }

    public Long getId() { return id; }
    public Long getEquipamentoId() { return equipamentoId; }
    public String getEquipamentoNome() { return equipamentoNome; }
    public String getEquipamentoCategoria() { return equipamentoCategoria; }
    public Integer getQuantidade() { return quantidade; }
    public BigDecimal getValorDiariaSnapshot() { return valorDiariaSnapshot; }
    public String getObservacaoItem() { return observacaoItem; }
}
