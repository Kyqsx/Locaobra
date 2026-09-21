package com.locaobra.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AvaliacaoRequest {

    // Obrigatório na criação; na edição (PUT /{id}) o equipamento não muda e este campo é ignorado.
    private Long equipamentoId;

    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota mínima é 1")
    @Max(value = 5, message = "A nota máxima é 5")
    private Integer nota;

    @Size(max = 1000, message = "O comentário pode ter no máximo 1000 caracteres")
    private String comentario;

    public Long getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(Long equipamentoId) { this.equipamentoId = equipamentoId; }

    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
