package com.locaobra.dto.response;

import com.locaobra.entity.Avaliacao;

import java.time.LocalDateTime;

public class AvaliacaoResponse {

    private Long id;
    private Long equipamentoId;
    private Integer nota;
    private String comentario;
    // Só primeiro nome + inicial do sobrenome — a lista é pública e não
    // deve expor o nome completo (nem CPF/CNPJ) do cliente.
    private String autor;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static AvaliacaoResponse from(Avaliacao a) {
        AvaliacaoResponse r = new AvaliacaoResponse();
        r.id = a.getId();
        r.equipamentoId = a.getEquipamento() != null ? a.getEquipamento().getId() : null;
        r.nota = a.getNota();
        r.comentario = a.getComentario();
        r.autor = nomeReduzido(a.getCliente() != null ? a.getCliente().getNome() : null);
        r.criadoEm = a.getCriadoEm();
        r.atualizadoEm = a.getAtualizadoEm();
        return r;
    }

    // "Maria da Silva Souza" -> "Maria S."
    static String nomeReduzido(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) return "Cliente";
        String[] partes = nomeCompleto.trim().split("\\s+");
        if (partes.length == 1) return partes[0];
        String ultimo = partes[partes.length - 1];
        return partes[0] + " " + Character.toUpperCase(ultimo.charAt(0)) + ".";
    }

    public Long getId() { return id; }
    public Long getEquipamentoId() { return equipamentoId; }
    public Integer getNota() { return nota; }
    public String getComentario() { return comentario; }
    public String getAutor() { return autor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
