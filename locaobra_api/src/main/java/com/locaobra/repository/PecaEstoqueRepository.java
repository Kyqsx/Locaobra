package com.locaobra.repository;

import com.locaobra.entity.PecaEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PecaEstoqueRepository extends JpaRepository<PecaEstoque, Long> {

    boolean existsByCodigo(String codigo);
}
