package com.locaobra.enums;

public enum StatusPedido {
    SOLICITADO,   // cliente pediu pelo catálogo (é um orçamento)
    CONFIRMADO,   // consultor revisou e confirmou; aguardando análise de crédito
    APROVADO,     // crédito aprovado; pronto para virar expedição (próxima etapa)
    RECUSADO,     // consultor recusou o pedido
    REPROVADO,    // análise de crédito reprovou
    CANCELADO     // cliente (ou staff) cancelou
}
