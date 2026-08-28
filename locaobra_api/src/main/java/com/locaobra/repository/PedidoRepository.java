package com.locaobra.repository;

import com.locaobra.entity.Pedido;
import com.locaobra.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByCodigo(String codigo);

    List<Pedido> findByClienteId(Long clienteId);

    List<Pedido> findByStatus(StatusPedido status);

    List<Pedido> findByStatusIn(List<StatusPedido> statuses);

    // "Crédito utilizado" do cliente: soma do valor estimado de todo pedido
    // ainda em aberto (não recusado/reprovado/cancelado) — usado tanto pra
    // exibir no cadastro quanto pra travar novo pedido acima do limite.
    // Ver ClienteService.calcularCreditoUtilizado() / PedidoService.criar().
    @Query("SELECT COALESCE(SUM(p.valorTotalEstimado), 0) FROM Pedido p " +
            "WHERE p.cliente.id = :clienteId AND p.status IN :statuses")
    BigDecimal somarValorEstimadoPorClienteEStatus(
            @Param("clienteId") Long clienteId,
            @Param("statuses") List<StatusPedido> statuses);

    // Fila do conferente: pedidos com crédito APROVADO que ainda não têm
    // nenhuma expedição ativa (não CANCELADO) gerada a partir deles — mesmo
    // padrão usado em ExpedicaoRepository.findEntregasConcluidasSemColetaAtiva().
    // Se a expedição gerada for cancelada depois, o pedido volta a aparecer
    // aqui, pra o conferente poder gerar uma nova.
    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM Pedido p WHERE p.status = com.locaobra.enums.StatusPedido.APROVADO " +
        "AND NOT EXISTS (SELECT 1 FROM Expedicao e WHERE e.pedido = p AND e.status <> com.locaobra.enums.StatusExpedicao.CANCELADO)"
    )
    List<Pedido> findAprovadosSemExpedicaoAtiva();
}
