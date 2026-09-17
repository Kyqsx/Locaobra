package com.locaobra.dto.response;

import com.locaobra.entity.ItemExpedicao;

import java.time.LocalDateTime;

public class ItemExpedicaoResponse {

    private Long id;
    private Long unidadeId;
    private String codigoPatrimonio;
    private Long equipamentoId;
    private String equipamentoNome;
    private Integer quantidade;
    private String observacaoItem;
    private LocalDateTime criadoEm;

    public static ItemExpedicaoResponse from(ItemExpedicao item) {
        ItemExpedicaoResponse r = new ItemExpedicaoResponse();
        r.id = item.getId();
        r.unidadeId = item.getUnidade() != null ? item.getUnidade().getId() : null;
        r.codigoPatrimonio = item.getUnidade() != null ? item.getUnidade().getCodigoPatrimonio() : null;
        r.equipamentoId = item.getEquipamento() != null ? item.getEquipamento().getId() : null;
        r.equipamentoNome = item.getEquipamento() != null ? item.getEquipamento().getNome() : null;
        r.quantidade = item.getQuantidade();
        r.observacaoItem = item.getObservacaoItem();
        r.criadoEm = item.getCriadoEm();
        return r;
    }

    public Long getId() { return id; }
    public Long getUnidadeId() { return unidadeId; }
    public String getCodigoPatrimonio() { return codigoPatrimonio; }
    public Long getEquipamentoId() { return equipamentoId; }
    public String getEquipamentoNome() { return equipamentoNome; }
    public Integer getQuantidade() { return quantidade; }
    public String getObservacaoItem() { return observacaoItem; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}