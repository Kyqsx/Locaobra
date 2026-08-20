package com.locaobra.repository;

import com.locaobra.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByDestinatarioTipoOrderByCriadaEmDesc(String destinatarioTipo);

    List<Notificacao> findByDestinatarioTipoAndLidaFalseOrderByCriadaEmDesc(String destinatarioTipo);

    long countByDestinatarioTipoAndLidaFalse(String destinatarioTipo);
}