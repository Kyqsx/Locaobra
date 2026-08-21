package com.locaobra.dto.request;

// Usado tanto pelo consultor (recusar) quanto pelo analista de crédito
// (reprovar) e também na confirmação (motivo fica nulo/observação opcional).
public class PedidoDecisaoRequest {

    private String motivo;
    private String observacoes;

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
