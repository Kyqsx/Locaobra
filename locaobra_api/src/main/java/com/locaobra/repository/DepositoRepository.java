package com.locaobra.repository;

import com.locaobra.entity.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositoRepository extends JpaRepository<Deposito, Long> {

    Optional<Deposito> findByNome(String nome);

    boolean existsByNome(String nome);

    List<Deposito> findByAtivoTrue();
}
