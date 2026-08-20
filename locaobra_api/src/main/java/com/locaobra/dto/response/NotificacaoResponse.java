package com.locaobra.dto.response;

import com.locaobra.entity.Notificacao;

import java.time.LocalDateTime;

public class NotificacaoResponse {

    private Long id;
    private String tipo;
    private String titulo;
    private String mensagem;
    private String destinatarioTipo;
    private Long destinatarioId;
    private String referenciaTipo;
    private Long referenciaId;
    private boolean lida;
    private LocalDateTime criadaEm;

    public static NotificacaoResponse from(Notificacao n) {
        NotificacaoResponse r = new NotificacaoResponse();
        r.id = n.getId();
        r.tipo = n.getTipo();
        r.titulo = n.getTitulo();
        r.mensagem = n.getMensagem();
        r.destinatarioTipo = n.getDestinatarioTipo();
        r.destinatarioId = n.getDestinatarioId();
        r.referenciaTipo = n.getReferenciaTipo();
        r.referenciaId = n.getReferenciaId();
        r.lida = n.isLida();
        r.criadaEm = n.getCriadaEm();
        return r;
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public String getDestinatarioTipo() { return destinatarioTipo; }
    public Long getDestinatarioId() { return destinatarioId; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public Long getReferenciaId() { return referenciaId; }
    public boolean isLida() { return lida; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}