package com.locaobra.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Imagem de equipamento enviada ao frontend: a URL do arquivo original
 * (que nunca é recortado) + o ponto focal do enquadramento 1:1
 * (percentuais 0–100 usados como object-position na exibição).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImagemEquipamentoResponse {

    private String url;
    private Integer focoX;
    private Integer focoY;

    public static ImagemEquipamentoResponse from(String url, Integer focoX, Integer focoY) {
        ImagemEquipamentoResponse r = new ImagemEquipamentoResponse();
        r.url = url;
        r.focoX = focoX;
        r.focoY = focoY;
        return r;
    }

    public String getUrl() { return url; }
    public Integer getFocoX() { return focoX; }
    public Integer getFocoY() { return focoY; }
}