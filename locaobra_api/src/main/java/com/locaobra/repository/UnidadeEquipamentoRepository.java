package com.locaobra.repository;

import com.locaobra.entity.UnidadeEquipamento;
import com.locaobra.enums.StatusUnidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnidadeEquipamentoRepository extends JpaRepository<UnidadeEquipamento, Long> {

    List<UnidadeEquipamento> findByEquipamentoId(Long equipamentoId);

    @Query("SELECT u FROM UnidadeEquipamento u WHERE u.horimetroAtual IS NOT NULL " +
           "AND u.horimetroLimiteManutencao IS NOT NULL " +
           "AND u.horimetroAtual >= u.horimetroLimiteManutencao " +
           "AND u.status <> com.locaobra.enums.StatusUnidade.EM_MANUTENCAO")
    List<UnidadeEquipamento> findComAlertaManutencaoPreventiva();

    List<UnidadeEquipamento> findByEquipamentoIdAndStatus(Long equipamentoId, StatusUnidade status);

    long countByEquipamentoIdAndStatus(Long equipamentoId, StatusUnidade status);

    boolean existsByCodigoPatrimonio(String codigoPatrimonio);

    boolean existsByNumeroDeSerie(String numeroDeSerie);

    void deleteByEquipamentoId(Long equipamentoId);
}
