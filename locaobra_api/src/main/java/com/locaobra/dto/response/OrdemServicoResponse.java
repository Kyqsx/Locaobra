package com.locaobra.dto.response;

import com.locaobra.entity.OrdemServico;
import com.locaobra.enums.StatusOrdemServico;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdemServicoResponse {

    private Long id;
    private Long unidadeId;
    private String unidadeCodigoPatrimonio;
    private String equipamentoNome;
    private Long tecnicoId;
    private String tecnicoNome;
    private StatusOrdemServico status;
    private String diagnostico;
    private String observacoes;
    private Double horimetroRegistrado;
    private LocalDateTime abertaEm;
    private LocalDateTime iniciadaEm;
    private LocalDateTime concluidaEm;
    private List<ItemOrdemServicoResponse> itens;

    public static OrdemServicoResponse from(OrdemServico os) {
        OrdemServicoResponse r = new OrdemServicoResponse();
        r.id = os.getId();
        r.unidadeId = os.getUnidade() != null ? os.getUnidade().getId() : null;
        r.unidadeCodigoPatrimonio = os.getUnidade() != null ? os.getUnidade().getCodigoPatrimonio() : null;
        r.equipamentoNome = (os.getUnidade() != null && os.getUnidade().getEquipamento() != null)
                ? os.getUnidade().getEquipamento().getNome() : null;
        r.tecnicoId = os.getTecnico() != null ? os.getTecnico().getId() : null;
        r.tecnicoNome = os.getTecnico() != null ? os.getTecnico().getNome() : null;
        r.status = os.getStatus();
        r.diagnostico = os.getDiagnostico();
        r.observacoes = os.getObservacoes();
        r.horimetroRegistrado = os.getHorimetroRegistrado();
        r.abertaEm = os.getAbertaEm();
        r.iniciadaEm = os.getIniciadaEm();
        r.concluidaEm = os.getConcluidaEm();
        if (os.getItens() != null) {
            r.itens = os.getItens().stream()
                    .map(ItemOrdemServicoResponse::from)
                    .collect(Collectors.toList());
        }
        return r;
    }

    public Long getId() { return id; }
    public Long getUnidadeId() { return unidadeId; }
    public String getUnidadeCodigoPatrimonio() { return unidadeCodigoPatrimonio; }
    public String getEquipamentoNome() { return equipamentoNome; }
    public Long getTecnicoId() { return tecnicoId; }
    public String getTecnicoNome() { return tecnicoNome; }
    public StatusOrdemServico getStatus() { return status; }
    public String getDiagnostico() { return diagnostico; }
    public String getObservacoes() { return observacoes; }
    public Double getHorimetroRegistrado() { return horimetroRegistrado; }
    public LocalDateTime getAbertaEm() { return abertaEm; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public List<ItemOrdemServicoResponse> getItens() { return itens; }
}
