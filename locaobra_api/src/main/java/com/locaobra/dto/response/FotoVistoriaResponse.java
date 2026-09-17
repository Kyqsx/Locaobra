package com.locaobra.dto.response;

import com.locaobra.entity.FotoVistoria;

import java.time.LocalDateTime;

public class FotoVistoriaResponse {

    private Long id;
    private String url;
    private String legenda;
    private LocalDateTime criadoEm;

    public static FotoVistoriaResponse from(FotoVistoria foto) {
        FotoVistoriaResponse r = new FotoVistoriaResponse();
        r.id = foto.getId();
        r.url = foto.getUrl();
        r.legenda = foto.getLegenda();
        r.criadoEm = foto.getCriadoEm();
        return r;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public String getLegenda() { return legenda; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}