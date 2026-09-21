package com.locaobra.dto.response;

// Situação do cliente logado em relação a um equipamento: já avaliou?
// pode avaliar? (só quem já recebeu o equipamento pode).
public class MinhaAvaliacaoResponse {

    private boolean podeAvaliar;
    private String motivo;               // preenchido quando podeAvaliar = false
    private AvaliacaoResponse avaliacao; // a avaliação existente, se houver

    public MinhaAvaliacaoResponse(boolean podeAvaliar, String motivo, AvaliacaoResponse avaliacao) {
        this.podeAvaliar = podeAvaliar;
        this.motivo = motivo;
        this.avaliacao = avaliacao;
    }

    public boolean isPodeAvaliar() { return podeAvaliar; }
    public String getMotivo() { return motivo; }
    public AvaliacaoResponse getAvaliacao() { return avaliacao; }
}
