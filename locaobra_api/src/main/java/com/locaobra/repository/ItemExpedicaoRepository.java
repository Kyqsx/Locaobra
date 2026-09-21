package com.locaobra.repository;

import com.locaobra.entity.ItemExpedicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemExpedicaoRepository extends JpaRepository<ItemExpedicao, Long> {

    List<ItemExpedicao> findByExpedicaoId(Long expedicaoId);

    List<ItemExpedicao> findByUnidadeId(Long unidadeId);

    List<ItemExpedicao> findByEquipamentoId(Long equipamentoId);

    void deleteByExpedicaoId(Long expedicaoId);

    // Quantas vezes esse cliente já DEVOLVEU esse equipamento: itens de COLETA
    // com check-in concluído (CONCLUIDO = o equipamento voltou pro depósito).
    // É o que libera o cliente a avaliar o equipamento.
    @Query("SELECT COUNT(ie) FROM ItemExpedicao ie " +
           "WHERE ie.equipamento.id = :equipamentoId " +
           "AND ie.expedicao.cliente.id = :clienteId " +
           "AND ie.expedicao.tipo = com.locaobra.enums.TipoExpedicao.COLETA " +
           "AND ie.expedicao.status = com.locaobra.enums.StatusExpedicao.CONCLUIDO")
    long contarColetasConcluidas(@Param("clienteId") Long clienteId, @Param("equipamentoId") Long equipamentoId);
}
