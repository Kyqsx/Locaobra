package com.locaobra.repository;

import com.locaobra.entity.OrdemServico;
import com.locaobra.enums.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findByStatus(StatusOrdemServico status);

    List<OrdemServico> findByStatusIn(List<StatusOrdemServico> status);

    List<OrdemServico> findByUnidadeId(Long unidadeId);

    boolean existsByUnidadeIdAndStatusIn(Long unidadeId, List<StatusOrdemServico> status);
}
