package com.locaobra.repository;

import com.locaobra.entity.DepartamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartamentoRepository extends JpaRepository<DepartamentoEntity, Long> {

    Optional<DepartamentoEntity> findByNome(String nome);

    boolean existsByNome(String nome);

    List<DepartamentoEntity> findByAtivoTrue();
}