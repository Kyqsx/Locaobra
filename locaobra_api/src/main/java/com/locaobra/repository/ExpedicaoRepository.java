package com.locaobra.repository;

import com.locaobra.entity.Expedicao;
import com.locaobra.enums.StatusExpedicao;
import com.locaobra.enums.TipoExpedicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    Optional<Expedicao> findByCodigo(String codigo);

    List<Expedicao> findByDataProgramada(LocalDate data);

    List<Expedicao> findByDataProgramadaBetween(LocalDate inicio, LocalDate fim);

    List<Expedicao> findByStatus(StatusExpedicao status);

    List<Expedicao> findByTipo(TipoExpedicao tipo);

    List<Expedicao> findByMotoristaId(Long motoristaId);

    List<Expedicao> findByClienteId(Long clienteId);

    List<Expedicao> findByStatusIn(List<StatusExpedicao> statuses);

    // Entregas já confirmadas no local do cliente (status ENTREGUE — passo 3,
    // fim de linha da ENTREGA) que ainda não têm nenhuma coleta ativa (não
    // cancelada) apontando pra elas via entregaOrigem — candidatas a aparecer
    // no select de "qual entrega vou buscar" na tela de nova expedição do tipo COLETA.
    @org.springframework.data.jpa.repository.Query(
        "SELECT e FROM Expedicao e WHERE e.tipo = com.locaobra.enums.TipoExpedicao.ENTREGA " +
        "AND e.status = com.locaobra.enums.StatusExpedicao.ENTREGUE " +
        "AND NOT EXISTS (SELECT 1 FROM Expedicao c WHERE c.entregaOrigem = e AND c.status <> com.locaobra.enums.StatusExpedicao.CANCELADO)"
    )
    List<Expedicao> findEntregasConcluidasSemColetaAtiva();

    boolean existsByEntregaOrigemIdAndStatusNot(Long entregaOrigemId, StatusExpedicao status);

    // Usado pra impedir gerar duas expedições ativas pro mesmo pedido: se já
    // existe uma não-CANCELADO vinculada, o pedido não deve mais aparecer na
    // fila do conferente (ver PedidoRepository.findAprovadosSemExpedicaoAtiva).
    boolean existsByPedidoIdAndStatusNot(Long pedidoId, StatusExpedicao status);
}