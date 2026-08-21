package com.locaobra.repository;

import com.locaobra.entity.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    List<ItemPedido> findByPedidoId(Long pedidoId);

    List<ItemPedido> findByEquipamentoId(Long equipamentoId);

    void deleteByPedidoId(Long pedidoId);
}
