package com.locaobra.enums;

public enum SituacaoCredito {
    EM_ANALISE, // padrão de um cliente recém-cadastrado, sem avaliação ainda
    LIBERADO,   // pode fazer pedidos normalmente, respeitando o limite
    BLOQUEADO   // não pode fazer novos pedidos, independente do limite
}
