package com.locaobra.entity;

import com.locaobra.enums.SituacaoCredito;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf_cnpj", nullable = false, length = 20)
    private String cpfCnpj;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false)
    private Boolean ativo = true;

    // ===================== CRÉDITO =====================
    // Todo cliente novo nasce EM_ANALISE (sem limite ainda) — só pode fazer
    // pedidos depois que um analista de credenciamento/financeiro libera.
    // O "crédito utilizado" NÃO é uma coluna: é calculado na hora (soma do
    // valorTotalEstimado dos pedidos ainda em aberto do cliente), pra nunca
    // ficar desatualizado — ver ClienteService.calcularCreditoUtilizado().
    @Column(name = "limite_credito", precision = 12, scale = 2)
    private BigDecimal limiteCredito;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'EM_ANALISE'")
    @Column(name = "situacao_credito", nullable = false, length = 20)
    private SituacaoCredito situacaoCredito = SituacaoCredito.EM_ANALISE;

    // Anotações internas do analista (ex.: histórico de inadimplência,
    // motivo do bloqueio) — não aparece pro cliente, só pra equipe.
    @Column(name = "observacoes_credito", length = 1000)
    private String observacoesCredito;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public SituacaoCredito getSituacaoCredito() {
        return situacaoCredito;
    }

    public void setSituacaoCredito(SituacaoCredito situacaoCredito) {
        this.situacaoCredito = situacaoCredito;
    }

    public String getObservacoesCredito() {
        return observacoesCredito;
    }

    public void setObservacoesCredito(String observacoesCredito) {
        this.observacoesCredito = observacoesCredito;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
