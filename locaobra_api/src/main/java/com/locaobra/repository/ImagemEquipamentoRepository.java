package com.locaobra.repository;

import com.locaobra.entity.ImagemEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImagemEquipamentoRepository extends JpaRepository<ImagemEquipamento, Long> {

    List<ImagemEquipamento> findByEquipamentoIdOrderByOrdem(Long equipamentoId);

    void deleteByEquipamentoId(Long equipamentoId);

    ImagemEquipamento findByEquipamentoIdAndUrl(Long equipamentoId, String url);

    void deleteByEquipamentoIdAndUrl(Long equipamentoId, String url);
}
