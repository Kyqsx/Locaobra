package com.locaobra.dto.request;

public class ItemPedidoRequest {

    private Long equipamentoId;
    private Integer quantidade;
    private String observacaoItem;

    public Long getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(Long equipamentoId) { this.equipamentoId = equipamentoId; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public String getObservacaoItem() { return observacaoItem; }
    public void setObservacaoItem(String observacaoItem) { this.observacaoItem = observacaoItem; }
}
