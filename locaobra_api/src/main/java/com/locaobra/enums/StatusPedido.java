package com.locaobra.enums;

public enum StatusPedido {
    SOLICITADO,   // cliente pediu pelo catálogo (é um orçamento)
    APROVADO,     // consultor confirmou o pedido; pronto para virar expedição
    RECUSADO,     // consultor recusou o pedido
    CANCELADO     // cliente (ou staff) cancelou
}
