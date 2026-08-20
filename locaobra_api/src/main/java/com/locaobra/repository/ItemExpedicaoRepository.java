package com.locaobra.repository;

import com.locaobra.entity.ItemExpedicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemExpedicaoRepository extends JpaRepository<ItemExpedicao, Long> {

    List<ItemExpedicao> findByExpedicaoId(Long expedicaoId);

    List<ItemExpedicao> findByUnidadeId(Long unidadeId);

    List<ItemExpedicao> findByEquipamentoId(Long equipamentoId);

    void deleteByExpedicaoId(Long expedicaoId);
}