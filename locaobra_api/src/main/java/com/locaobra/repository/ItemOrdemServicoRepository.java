package com.locaobra.repository;

import com.locaobra.entity.ItemOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemOrdemServicoRepository extends JpaRepository<ItemOrdemServico, Long> {

    List<ItemOrdemServico> findByOrdemServicoId(Long ordemServicoId);
}
