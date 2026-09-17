package com.locaobra.dto.response;

import com.locaobra.entity.ItemOrdemServico;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemOrdemServicoResponse {

    private Long id;
    private Long pecaId;
    private String pecaNome;
    private String pecaCodigo;
    private Integer quantidade;
    private LocalDateTime criadoEm;

    public static ItemOrdemServicoResponse from(ItemOrdemServico i) {
        ItemOrdemServicoResponse r = new ItemOrdemServicoResponse();
        r.id = i.getId();
        r.pecaId = i.getPeca() != null ? i.getPeca().getId() : null;
        r.pecaNome = i.getPeca() != null ? i.getPeca().getNome() : null;
        r.pecaCodigo = i.getPeca() != null ? i.getPeca().getCodigo() : null;
        r.quantidade = i.getQuantidade();
        r.criadoEm = i.getCriadoEm();
        return r;
    }

    public Long getId() { return id; }
    public Long getPecaId() { return pecaId; }
    public String getPecaNome() { return pecaNome; }
    public String getPecaCodigo() { return pecaCodigo; }
    public Integer getQuantidade() { return quantidade; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
