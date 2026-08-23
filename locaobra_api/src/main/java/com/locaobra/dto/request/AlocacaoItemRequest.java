package com.locaobra.dto.request;

public class AlocacaoItemRequest {

    private Long itemPedidoId;
    private Long depositoId;

    public Long getItemPedidoId() { return itemPedidoId; }
    public void setItemPedidoId(Long itemPedidoId) { this.itemPedidoId = itemPedidoId; }

    public Long getDepositoId() { return depositoId; }
    public void setDepositoId(Long depositoId) { this.depositoId = depositoId; }
}
