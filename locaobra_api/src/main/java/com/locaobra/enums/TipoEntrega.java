package com.locaobra.enums;

// Como o cliente quer receber o equipamento.
// ENTREGA  -> a Locaobra leva até o endereço informado no pedido (tem frete).
// RETIRADA -> o cliente busca o equipamento no depósito combinado (frete zero).
public enum TipoEntrega {
    ENTREGA,
    RETIRADA
}