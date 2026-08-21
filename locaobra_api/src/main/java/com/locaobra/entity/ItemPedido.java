package com.locaobra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "itens_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    @Column(nullable = false)
    private Integer quantidade;

    // Valor da diária no momento em que o pedido foi feito — não muda se o
    // preço do equipamento mudar depois (o cliente vê e paga o que pediu).
    @Column(name = "valor_diaria_snapshot", nullable = false)
    private BigDecimal valorDiariaSnapshot;

    @Column(name = "observacao_item", length = 500)
    private String observacaoItem;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Equipamento getEquipamento() { return equipamento; }
    public void setEquipamento(Equipamento equipamento) { this.equipamento = equipamento; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public BigDecimal getValorDiariaSnapshot() { return valorDiariaSnapshot; }
    public void setValorDiariaSnapshot(BigDecimal valorDiariaSnapshot) { this.valorDiariaSnapshot = valorDiariaSnapshot; }

    public String getObservacaoItem() { return observacaoItem; }
    public void setObservacaoItem(String observacaoItem) { this.observacaoItem = observacaoItem; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
