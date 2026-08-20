package com.locaobra.entity;

import com.locaobra.enums.TipoVistoria;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vistorias")
public class Vistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expedicao_id", nullable = false)
    private Expedicao expedicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private UnidadeEquipamento unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVistoria tipo;

    @Column(name = "condicao_geral", length = 20)
    private String condicaoGeral;

    @Column(name = "avarias_existentes", length = 2000)
    private String avariasExistentes;

    @Column(name = "danos_causados", length = 2000)
    private String danosCausados;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "realizada_em")
    private LocalDateTime realizadaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "vistoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FotoVistoria> fotos = new ArrayList<>();

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

    public TipoVistoria getTipo() { return tipo; }
    public void setTipo(TipoVistoria tipo) { this.tipo = tipo; }

    public String getCondicaoGeral() { return condicaoGeral; }
    public void setCondicaoGeral(String condicaoGeral) { this.condicaoGeral = condicaoGeral; }

    public String getAvariasExistentes() { return avariasExistentes; }
    public void setAvariasExistentes(String avariasExistentes) { this.avariasExistentes = avariasExistentes; }

    public String getDanosCausados() { return danosCausados; }
    public void setDanosCausados(String danosCausados) { this.danosCausados = danosCausados; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getRealizadaEm() { return realizadaEm; }
    public void setRealizadaEm(LocalDateTime realizadaEm) { this.realizadaEm = realizadaEm; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public List<FotoVistoria> getFotos() { return fotos; }
    public void setFotos(List<FotoVistoria> fotos) { this.fotos = fotos; }
}