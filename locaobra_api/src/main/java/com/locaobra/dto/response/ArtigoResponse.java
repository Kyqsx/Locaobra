package com.locaobra.dto.response;

import com.locaobra.entity.Artigo;

import java.time.LocalDateTime;

public class ArtigoResponse {

    private Long id;
    private String titulo;
    private String slug;
    private String resumo;
    private String conteudo;
    private String imagemCapa;
    private String autor;
    private Boolean publicado;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static ArtigoResponse from(Artigo a) {
        ArtigoResponse r = new ArtigoResponse();
        r.id = a.getId();
        r.titulo = a.getTitulo();
        r.slug = a.getSlug();
        r.resumo = a.getResumo();
        r.conteudo = a.getConteudo();
        r.imagemCapa = a.getImagemCapa();
        r.autor = a.getAutor();
        r.publicado = a.getPublicado();
        r.criadoEm = a.getCriadoEm();
        r.atualizadoEm = a.getAtualizadoEm();
        return r;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getSlug() { return slug; }
    public String getResumo() { return resumo; }
    public String getConteudo() { return conteudo; }
    public String getImagemCapa() { return imagemCapa; }
    public String getAutor() { return autor; }
    public Boolean getPublicado() { return publicado; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
