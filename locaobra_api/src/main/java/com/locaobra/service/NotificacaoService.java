package com.locaobra.service;

import com.locaobra.dto.response.NotificacaoResponse;
import com.locaobra.entity.Notificacao;
import com.locaobra.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    @Transactional
    public NotificacaoResponse criar(String tipo, String titulo, String mensagem,
                                     String destinatarioTipo, Long destinatarioId,
                                     String referenciaTipo, Long referenciaId) {
        Notificacao notificacao = new Notificacao();
        notificacao.setTipo(tipo);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacao.setDestinatarioTipo(destinatarioTipo);
        notificacao.setDestinatarioId(destinatarioId);
        notificacao.setReferenciaTipo(referenciaTipo);
        notificacao.setReferenciaId(referenciaId);
        notificacao = notificacaoRepository.save(notificacao);
        return NotificacaoResponse.from(notificacao);
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listarPorDestinatario(String destinatarioTipo) {
        return notificacaoRepository.findByDestinatarioTipoOrderByCriadaEmDesc(destinatarioTipo)
                .stream()
                .map(NotificacaoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listarNaoLidas(String destinatarioTipo) {
        return notificacaoRepository.findByDestinatarioTipoAndLidaFalseOrderByCriadaEmDesc(destinatarioTipo)
                .stream()
                .map(NotificacaoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(String destinatarioTipo) {
        return notificacaoRepository.countByDestinatarioTipoAndLidaFalse(destinatarioTipo);
    }

    @Transactional
    public void marcarLida(Long id) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new com.locaobra.exception.ResourceNotFoundException("Notificação não encontrada: " + id));
        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);
    }

    @Transactional
    public void marcarTodasLidas(String destinatarioTipo) {
        List<Notificacao> naoLidas = notificacaoRepository.findByDestinatarioTipoAndLidaFalseOrderByCriadaEmDesc(destinatarioTipo);
        naoLidas.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(naoLidas);
    }
}