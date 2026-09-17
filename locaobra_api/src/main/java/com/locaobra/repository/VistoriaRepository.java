package com.locaobra.repository;

import com.locaobra.entity.Vistoria;
import com.locaobra.enums.TipoVistoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {

    List<Vistoria> findByExpedicaoId(Long expedicaoId);

    List<Vistoria> findByUnidadeId(Long unidadeId);

    List<Vistoria> findByExpedicaoIdAndTipo(Long expedicaoId, TipoVistoria tipo);

    List<Vistoria> findByExpedicaoIdAndUnidadeId(Long expedicaoId, Long unidadeId);

    boolean existsByExpedicaoIdAndUnidadeIdAndTipo(Long expedicaoId, Long unidadeId, TipoVistoria tipo);
}