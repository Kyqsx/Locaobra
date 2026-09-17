package com.locaobra.dto.response;

import com.locaobra.entity.PecaEstoque;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PecaEstoqueResponse {

    private Long id;
    private String codigo;
    private String nome;
    private Integer quantidadeEmEstoque;
    private String unidadeMedida;
    private Integer estoqueMinimo;
    private boolean estoqueBaixo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static PecaEstoqueResponse from(PecaEstoque p) {
        PecaEstoqueResponse r = new PecaEstoqueResponse();
        r.id = p.getId();
        r.codigo = p.getCodigo();
        r.nome = p.getNome();
        r.quantidadeEmEstoque = p.getQuantidadeEmEstoque();
        r.unidadeMedida = p.getUnidadeMedida();
        r.estoqueMinimo = p.getEstoqueMinimo();
        r.estoqueBaixo = p.getEstoqueMinimo() != null
                && p.getQuantidadeEmEstoque() != null
                && p.getQuantidadeEmEstoque() <= p.getEstoqueMinimo();
        r.criadoEm = p.getCriadoEm();
        r.atualizadoEm = p.getAtualizadoEm();
        return r;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public Integer getQuantidadeEmEstoque() { return quantidadeEmEstoque; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public boolean isEstoqueBaixo() { return estoqueBaixo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
