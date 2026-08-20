package com.locaobra.dto.request;

public class CargoRequest {

    private String nome;
    private String descricao;
    private Double salarioPadrao;
    private Long departamentoId;
    private String requisitos;
    private Boolean ativo;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Double getSalarioPadrao() { return salarioPadrao; }
    public void setSalarioPadrao(Double salarioPadrao) { this.salarioPadrao = salarioPadrao; }
    public Long getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(Long departamentoId) { this.departamentoId = departamentoId; }
    public String getRequisitos() { return requisitos; }
    public void setRequisitos(String requisitos) { this.requisitos = requisitos; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}