package com.locaobra.dto.response;

import java.util.List;
import java.util.Map;

// Resumo público das avaliações de um equipamento + a lista completa.
public class AvaliacoesEquipamentoResponse {

    private Double media;                    // null quando ainda não há avaliações
    private long total;
    private Map<Integer, Long> distribuicao; // nota (1..5) -> quantidade
    private List<AvaliacaoResponse> avaliacoes;

    public AvaliacoesEquipamentoResponse(Double media, long total,
                                         Map<Integer, Long> distribuicao,
                                         List<AvaliacaoResponse> avaliacoes) {
        this.media = media;
        this.total = total;
        this.distribuicao = distribuicao;
        this.avaliacoes = avaliacoes;
    }

    public Double getMedia() { return media; }
    public long getTotal() { return total; }
    public Map<Integer, Long> getDistribuicao() { return distribuicao; }
    public List<AvaliacaoResponse> getAvaliacoes() { return avaliacoes; }
}
