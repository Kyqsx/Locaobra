package com.locaobra.dto.request;

import java.util.ArrayList;
import java.util.List;

// Usado pelo consultor pra confirmar um pedido SOLICITADO. Precisa dizer de
// qual depósito cada item vai sair — normalmente é a sugestão do sistema
// (GET /api/pedidos/{id}/sugestao-depositos) aceita como veio, mas o
// consultor pode sobrescrever manualmente antes de confirmar.
public class ConfirmarPedidoRequest {

    private String observacoes;
    private List<AlocacaoItemRequest> alocacoes = new ArrayList<>();

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public List<AlocacaoItemRequest> getAlocacoes() { return alocacoes; }
    public void setAlocacoes(List<AlocacaoItemRequest> alocacoes) { this.alocacoes = alocacoes; }
}
