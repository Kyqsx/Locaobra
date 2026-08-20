package com.locaobra.repository;

import com.locaobra.entity.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {

    Optional<Cargo> findByNome(String nome);

    boolean existsByNome(String nome);

    List<Cargo> findByAtivoTrue();

    List<Cargo> findByDepartamentoId(Long departamentoId);
}