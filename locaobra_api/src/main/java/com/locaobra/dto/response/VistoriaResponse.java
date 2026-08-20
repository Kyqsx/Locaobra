package com.locaobra.dto.response;

import com.locaobra.entity.FotoVistoria;
import com.locaobra.entity.Vistoria;
import com.locaobra.enums.TipoVistoria;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VistoriaResponse {

    private Long id;
    private Long expedicaoId;
    private Long unidadeId;
    private String codigoPatrimonio;
    private TipoVistoria tipo;
    private String condicaoGeral;
    private String avariasExistentes;
    private String danosCausados;
    private String observacoes;
    private LocalDateTime realizadaEm;
    private List<FotoVistoriaResponse> fotos;

    public static VistoriaResponse from(Vistoria v) {
        VistoriaResponse r = new VistoriaResponse();
        r.id = v.getId();
        r.expedicaoId = v.getExpedicao() != null ? v.getExpedicao().getId() : null;
        r.unidadeId = v.getUnidade() != null ? v.getUnidade().getId() : null;
        r.codigoPatrimonio = v.getUnidade() != null ? v.getUnidade().getCodigoPatrimonio() : null;
        r.tipo = v.getTipo();
        r.condicaoGeral = v.getCondicaoGeral();
        r.avariasExistentes = v.getAvariasExistentes();
        r.danosCausados = v.getDanosCausados();
        r.observacoes = v.getObservacoes();
        r.realizadaEm = v.getRealizadaEm();
        // Não lemos v.getFotos() aqui: essa coleção é @OneToMany(orphanRemoval = true) e um
        // clear() feito em outro lugar apagaria as fotos no banco. Use from(v, fotos) para
        // montar a resposta com uma lista buscada direto do repositório.
        r.fotos = Collections.emptyList();
        return r;
    }

    public static VistoriaResponse from(Vistoria v, List<FotoVistoria> fotos) {
        VistoriaResponse r = from(v);
        r.fotos = fotos != null
                ? fotos.stream().map(FotoVistoriaResponse::from).collect(Collectors.toList())
                : Collections.emptyList();
        return r;
    }

    public Long getId() { return id; }
    public Long getExpedicaoId() { return expedicaoId; }
    public Long getUnidadeId() { return unidadeId; }
    public String getCodigoPatrimonio() { return codigoPatrimonio; }
    public TipoVistoria getTipo() { return tipo; }
    public String getCondicaoGeral() { return condicaoGeral; }
    public String getAvariasExistentes() { return avariasExistentes; }
    public String getDanosCausados() { return danosCausados; }
    public String getObservacoes() { return observacoes; }
    public LocalDateTime getRealizadaEm() { return realizadaEm; }
    public List<FotoVistoriaResponse> getFotos() { return fotos; }
}