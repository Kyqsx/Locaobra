package com.locaobra.dto.response;

import com.locaobra.entity.UnidadeEquipamento;
import com.locaobra.enums.StatusUnidade;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnidadeEquipamentoResponse {

    private Long id;
    private Long equipamentoId;
    private String codigoPatrimonio;
    private String numeroDeSerie;
    private StatusUnidade status;
    private Double horimetroAtual;
    private Double horimetroLimiteManutencao;
    private boolean alertaManutencaoPreventiva;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static UnidadeEquipamentoResponse from(UnidadeEquipamento u) {
        UnidadeEquipamentoResponse r = new UnidadeEquipamentoResponse();
        r.id = u.getId();
        r.equipamentoId = u.getEquipamento() != null ? u.getEquipamento().getId() : null;
        r.codigoPatrimonio = u.getCodigoPatrimonio();
        r.numeroDeSerie = u.getNumeroDeSerie();
        r.status = u.getStatus();
        r.horimetroAtual = u.getHorimetroAtual();
        r.horimetroLimiteManutencao = u.getHorimetroLimiteManutencao();
        r.alertaManutencaoPreventiva = u.getHorimetroAtual() != null
                && u.getHorimetroLimiteManutencao() != null
                && u.getHorimetroAtual() >= u.getHorimetroLimiteManutencao();
        r.criadoEm = u.getCriadoEm();
        r.atualizadoEm = u.getAtualizadoEm();
        return r;
    }

    public Long getId() { return id; }
    public Long getEquipamentoId() { return equipamentoId; }
    public String getCodigoPatrimonio() { return codigoPatrimonio; }
    public String getNumeroDeSerie() { return numeroDeSerie; }
    public StatusUnidade getStatus() { return status; }
    public Double getHorimetroAtual() { return horimetroAtual; }
    public Double getHorimetroLimiteManutencao() { return horimetroLimiteManutencao; }
    public boolean isAlertaManutencaoPreventiva() { return alertaManutencaoPreventiva; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
