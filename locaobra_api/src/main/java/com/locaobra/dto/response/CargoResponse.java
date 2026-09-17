package com.locaobra.dto.response;

import com.locaobra.entity.Cargo;

import java.time.LocalDateTime;

public class CargoResponse {

    private Long id;
    private String nome;
    private String descricao;
    private Double salarioPadrao;
    private Long departamentoId;
    private String departamentoNome;
    private String requisitos;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static CargoResponse from(Cargo c) {
        CargoResponse r = new CargoResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.descricao = c.getDescricao();
        r.salarioPadrao = c.getSalarioPadrao();
        r.requisitos = c.getRequisitos();
        r.ativo = c.getAtivo();
        r.criadoEm = c.getCriadoEm();
        r.atualizadoEm = c.getAtualizadoEm();
        if (c.getDepartamento() != null) {
            r.departamentoId = c.getDepartamento().getId();
            r.departamentoNome = c.getDepartamento().getNome();
        }
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Double getSalarioPadrao() { return salarioPadrao; }
    public Long getDepartamentoId() { return departamentoId; }
    public String getDepartamentoNome() { return departamentoNome; }
    public String getRequisitos() { return requisitos; }
    public Boolean getAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}