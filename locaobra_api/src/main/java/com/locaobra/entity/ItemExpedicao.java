package com.locaobra.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "itens_expedicao")
public class ItemExpedicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expedicao_id", nullable = false)
    private Expedicao expedicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private UnidadeEquipamento unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    @Column(nullable = false)
    private Integer quantidade;

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

    public Expedicao getExpedicao() { return expedicao; }
    public void setExpedicao(Expedicao expedicao) { this.expedicao = expedicao; }

    public UnidadeEquipamento getUnidade() { return unidade; }
    public void setUnidade(UnidadeEquipamento unidade) { this.unidade = unidade; }

    public Equipamento getEquipamento() { return equipamento; }
    public void setEquipamento(Equipamento equipamento) { this.equipamento = equipamento; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public String getObservacaoItem() { return observacaoItem; }
    public void setObservacaoItem(String observacaoItem) { this.observacaoItem = observacaoItem; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}