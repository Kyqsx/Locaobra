package com.locaobra.repository;

import com.locaobra.entity.Artigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtigoRepository extends JpaRepository<Artigo, Long> {

    Optional<Artigo> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Artigo> findByPublicadoTrueOrderByCriadoEmDesc();

    List<Artigo> findAllByOrderByCriadoEmDesc();
}
