package com.locaobra.repository;

import com.locaobra.entity.Equipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    List<Equipamento> findByStatus(String status);

    List<Equipamento> findByCategoria(String categoria);
}
