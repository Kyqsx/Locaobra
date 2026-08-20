package com.locaobra.dto.request;

public class DiagnosticoRequest {

    private String diagnostico;
    private String observacoes;
    private Double horimetroRegistrado;
    private Long tecnicoId;

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public Double getHorimetroRegistrado() { return horimetroRegistrado; }
    public void setHorimetroRegistrado(Double horimetroRegistrado) { this.horimetroRegistrado = horimetroRegistrado; }

    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }
}
