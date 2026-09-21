package com.locaobra.repository;

import com.locaobra.entity.Avaliacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    // Traz o cliente junto (o nome aparece na lista) sem N+1.
    @EntityGraph(attributePaths = {"cliente"})
    List<Avaliacao> findByEquipamentoIdOrderByCriadoEmDesc(Long equipamentoId);

    Optional<Avaliacao> findByClienteIdAndEquipamentoId(Long clienteId, Long equipamentoId);

    boolean existsByClienteIdAndEquipamentoId(Long clienteId, Long equipamentoId);

    long countByEquipamentoId(Long equipamentoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.equipamento.id = :equipamentoId")
    Double mediaPorEquipamento(@Param("equipamentoId") Long equipamentoId);

    // [nota, quantidade] — alimenta as barras de distribuição (5★ ... 1★).
    @Query("SELECT a.nota, COUNT(a) FROM Avaliacao a WHERE a.equipamento.id = :equipamentoId GROUP BY a.nota")
    List<Object[]> distribuicaoPorEquipamento(@Param("equipamentoId") Long equipamentoId);

    // [equipamentoId, média, total] de TODOS os equipamentos avaliados — uma
    // consulta só pra decorar a listagem do catálogo.
    @Query("SELECT a.equipamento.id, AVG(a.nota), COUNT(a) FROM Avaliacao a GROUP BY a.equipamento.id")
    List<Object[]> resumoPorEquipamento();
}
