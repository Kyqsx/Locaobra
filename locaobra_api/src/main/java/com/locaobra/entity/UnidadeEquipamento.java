package com.locaobra.entity;

import com.locaobra.enums.StatusUnidade;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "unidades_equipamento")
public class UnidadeEquipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    // Onde essa unidade física está guardada — vínculo é sempre com a
    // unidade (patrimônio), nunca com o modelo de Equipamento. Nullable:
    // unidade pode não ter sido alocada a um depósito ainda.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;

    @Column(name = "codigo_patrimonio", unique = true, length = 100)
    private String codigoPatrimonio;

    @Column(name = "numero_serie", unique = true, length = 100)
    private String numeroDeSerie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusUnidade status = StatusUnidade.DISPONIVEL;

    @Column(name = "horimetro_atual")
    private Double horimetroAtual;

    @Column(name = "horimetro_limite_manutencao")
    private Double horimetroLimiteManutencao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Equipamento getEquipamento() {
        return equipamento;
    }

    public void setEquipamento(Equipamento equipamento) {
        this.equipamento = equipamento;
    }

    public Deposito getDeposito() {
        return deposito;
    }

    public void setDeposito(Deposito deposito) {
        this.deposito = deposito;
    }

    public String getCodigoPatrimonio() {
        return codigoPatrimonio;
    }

    public void setCodigoPatrimonio(String codigoPatrimonio) {
        this.codigoPatrimonio = codigoPatrimonio;
    }

    public String getNumeroDeSerie() {
        return numeroDeSerie;
    }

    public void setNumeroDeSerie(String numeroDeSerie) {
        this.numeroDeSerie = numeroDeSerie;
    }

    public StatusUnidade getStatus() {
        return status;
    }

    public void setStatus(StatusUnidade status) {
        this.status = status;
    }

    public Double getHorimetroAtual() {
        return horimetroAtual;
    }

    public void setHorimetroAtual(Double horimetroAtual) {
        this.horimetroAtual = horimetroAtual;
    }

    public Double getHorimetroLimiteManutencao() {
        return horimetroLimiteManutencao;
    }

    public void setHorimetroLimiteManutencao(Double horimetroLimiteManutencao) {
        this.horimetroLimiteManutencao = horimetroLimiteManutencao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
