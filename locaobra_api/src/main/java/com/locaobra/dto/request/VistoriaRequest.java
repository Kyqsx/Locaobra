package com.locaobra.dto.request;

import com.locaobra.enums.TipoVistoria;

import java.util.ArrayList;
import java.util.List;

public class VistoriaRequest {

    private Long unidadeId;
    private TipoVistoria tipo;
    private String condicaoGeral;
    private String avariasExistentes;
    private String danosCausados;
    private String observacoes;
    private List<String> fotos = new ArrayList<>();

    public Long getUnidadeId() { return unidadeId; }
    public void setUnidadeId(Long unidadeId) { this.unidadeId = unidadeId; }

    public TipoVistoria getTipo() { return tipo; }
    public void setTipo(TipoVistoria tipo) { this.tipo = tipo; }

    public String getCondicaoGeral() { return condicaoGeral; }
    public void setCondicaoGeral(String condicaoGeral) { this.condicaoGeral = condicaoGeral; }

    public String getAvariasExistentes() { return avariasExistentes; }
    public void setAvariasExistentes(String avariasExistentes) { this.avariasExistentes = avariasExistentes; }

    public String getDanosCausados() { return danosCausados; }
    public void setDanosCausados(String danosCausados) { this.danosCausados = danosCausados; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }
}