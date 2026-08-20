package com.locaobra.repository;

import com.locaobra.entity.FotoVistoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoVistoriaRepository extends JpaRepository<FotoVistoria, Long> {

    List<FotoVistoria> findByVistoriaId(Long vistoriaId);
}