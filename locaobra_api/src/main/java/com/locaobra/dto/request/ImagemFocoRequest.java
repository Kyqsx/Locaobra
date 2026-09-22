package com.locaobra.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do PUT /api/equipamentos/{id}/imagens/foco — reposiciona o ponto
 * focal (0–100) do enquadramento 1:1 de uma imagem já cadastrada.
 */
public class ImagemFocoRequest {

    @NotBlank(message = "URL da imagem é obrigatória")
    private String url;

    @NotNull(message = "focoX é obrigatório")
    @Min(value = 0, message = "focoX deve estar entre 0 e 100")
    @Max(value = 100, message = "focoX deve estar entre 0 e 100")
    private Integer focoX;

    @NotNull(message = "focoY é obrigatório")
    @Min(value = 0, message = "focoY deve estar entre 0 e 100")
    @Max(value = 100, message = "focoY deve estar entre 0 e 100")
    private Integer focoY;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getFocoX() { return focoX; }
    public void setFocoX(Integer focoX) { this.focoX = focoX; }
    public Integer getFocoY() { return focoY; }
    public void setFocoY(Integer focoY) { this.focoY = focoY; }
}