package com.locaobra.dto.request;

public class ItemOrdemServicoRequest {

    private Long pecaId;
    private Integer quantidade;

    public Long getPecaId() { return pecaId; }
    public void setPecaId(Long pecaId) { this.pecaId = pecaId; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
