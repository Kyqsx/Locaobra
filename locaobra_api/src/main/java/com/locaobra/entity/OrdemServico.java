package com.locaobra.entity;

import com.locaobra.enums.StatusOrdemServico;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordens_servico")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeEquipamento unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Funcionario tecnico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrdemServico status = StatusOrdemServico.ABERTA;

    @Column(name = "diagnostico", length = 2000)
    private String diagnostico;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "horimetro_registrado")
    private Double horimetroRegistrado;

    @Column(name = "aberta_em", nullable = false)
    private LocalDateTime abertaEm;

    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrdemServico> itens = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.abertaEm == null) this.abertaEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UnidadeEquipamento getUnidade() { return unidade; }
    public void setUnidade(UnidadeEquipamento unidade) { this.unidade = unidade; }

    public Funcionario getTecnico() { return tecnico; }
    public void setTecnico(Funcionario tecnico) { this.tecnico = tecnico; }

    public StatusOrdemServico getStatus() { return status; }
    public void setStatus(StatusOrdemServico status) { this.status = status; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public Double getHorimetroRegistrado() { return horimetroRegistrado; }
    public void setHorimetroRegistrado(Double horimetroRegistrado) { this.horimetroRegistrado = horimetroRegistrado; }

    public LocalDateTime getAbertaEm() { return abertaEm; }
    public void setAbertaEm(LocalDateTime abertaEm) { this.abertaEm = abertaEm; }

    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(LocalDateTime iniciadaEm) { this.iniciadaEm = iniciadaEm; }

    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public List<ItemOrdemServico> getItens() { return itens; }
    public void setItens(List<ItemOrdemServico> itens) { this.itens = itens; }
}
