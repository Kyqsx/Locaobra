package com.locaobra.dto.request;

import com.locaobra.enums.StatusUnidade;

public class UnidadeEquipamentoRequest {

    private String codigoPatrimonio;
    private String numeroDeSerie;
    private StatusUnidade status;
    private Double horimetroAtual;
    private Double horimetroLimiteManutencao;

    public String getCodigoPatrimonio() { return codigoPatrimonio; }
    public void setCodigoPatrimonio(String codigoPatrimonio) { this.codigoPatrimonio = codigoPatrimonio; }

    public String getNumeroDeSerie() { return numeroDeSerie; }
    public void setNumeroDeSerie(String numeroDeSerie) { this.numeroDeSerie = numeroDeSerie; }

    public StatusUnidade getStatus() { return status; }
    public void setStatus(StatusUnidade status) { this.status = status; }

    public Double getHorimetroAtual() { return horimetroAtual; }
    public void setHorimetroAtual(Double horimetroAtual) { this.horimetroAtual = horimetroAtual; }

    public Double getHorimetroLimiteManutencao() { return horimetroLimiteManutencao; }
    public void setHorimetroLimiteManutencao(Double horimetroLimiteManutencao) { this.horimetroLimiteManutencao = horimetroLimiteManutencao; }
}
