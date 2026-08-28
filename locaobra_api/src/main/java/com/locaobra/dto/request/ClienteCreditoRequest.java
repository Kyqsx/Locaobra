package com.locaobra.dto.request;

import com.locaobra.enums.SituacaoCredito;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Usado só pelo endpoint PATCH /api/clientes/{id}/credito — separado do
// ClienteRequest "de cadastro" porque quem edita é outro perfil (analista de
// credenciamento/financeiro), não quem cadastra o cliente.
public class ClienteCreditoRequest {

    @NotNull(message = "Situação de crédito é obrigatória")
    private SituacaoCredito situacaoCredito;

    // Nulo = sem limite definido ainda (cliente EM_ANALISE, por exemplo).
    private BigDecimal limiteCredito;

    private String observacoesCredito;

    public SituacaoCredito getSituacaoCredito() { return situacaoCredito; }
    public void setSituacaoCredito(SituacaoCredito situacaoCredito) { this.situacaoCredito = situacaoCredito; }

    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) { this.limiteCredito = limiteCredito; }

    public String getObservacoesCredito() { return observacoesCredito; }
    public void setObservacoesCredito(String observacoesCredito) { this.observacoesCredito = observacoesCredito; }
}
