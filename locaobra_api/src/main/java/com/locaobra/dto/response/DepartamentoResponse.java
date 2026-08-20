package com.locaobra.dto.response;

import com.locaobra.entity.DepartamentoEntity;

import java.time.LocalDateTime;

public class DepartamentoResponse {

    private Long id;
    private String nome;
    private String descricao;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static DepartamentoResponse from(DepartamentoEntity d) {
        DepartamentoResponse r = new DepartamentoResponse();
        r.id = d.getId();
        r.nome = d.getNome();
        r.descricao = d.getDescricao();
        r.ativo = d.getAtivo();
        r.criadoEm = d.getCriadoEm();
        r.atualizadoEm = d.getAtualizadoEm();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Boolean getAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}