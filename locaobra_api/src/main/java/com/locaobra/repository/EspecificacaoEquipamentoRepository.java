package com.locaobra.repository;

import com.locaobra.entity.EspecificacaoEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspecificacaoEquipamentoRepository extends JpaRepository<EspecificacaoEquipamento, Long> {

    List<EspecificacaoEquipamento> findByEquipamentoId(Long equipamentoId);

    void deleteByEquipamentoId(Long equipamentoId);
}
