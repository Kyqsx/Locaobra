package com.locaobra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "equipamentos")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(name = "valor_diaria", nullable = false)
    private BigDecimal valorDiaria;

    // ---- Dados logísticos (base do cálculo de frete) -------------------------
    // Peso bruto por unidade (kg) e dimensões da embalagem/estrutura (cm).
    // Opcional: enquanto não forem preenchidos, o frete usa um peso/dimensão
    // padrão conservador (ver FreteService) — nunca quebra o pedido.
    @Column(name = "peso_kg", precision = 10, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "comprimento_cm", precision = 10, scale = 2)
    private BigDecimal comprimentoCm;

    @Column(name = "largura_cm", precision = 10, scale = 2)
    private BigDecimal larguraCm;

    @Column(name = "altura_cm", precision = 10, scale = 2)
    private BigDecimal alturaCm;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ativo";

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "equipamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EspecificacaoEquipamento> especificacoes = new ArrayList<>();

    @OneToMany(mappedBy = "equipamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImagemEquipamento> imagens = new ArrayList<>();

    @OneToMany(mappedBy = "equipamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnidadeEquipamento> unidades = new ArrayList<>();

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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getValorDiaria() {
        return valorDiaria;
    }

    public void setValorDiaria(BigDecimal valorDiaria) {
        this.valorDiaria = valorDiaria;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getComprimentoCm() {
        return comprimentoCm;
    }

    public void setComprimentoCm(BigDecimal comprimentoCm) {
        this.comprimentoCm = comprimentoCm;
    }

    public BigDecimal getLarguraCm() {
        return larguraCm;
    }

    public void setLarguraCm(BigDecimal larguraCm) {
        this.larguraCm = larguraCm;
    }

    public BigDecimal getAlturaCm() {
        return alturaCm;
    }

    public void setAlturaCm(BigDecimal alturaCm) {
        this.alturaCm = alturaCm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<EspecificacaoEquipamento> getEspecificacoes() {
        return especificacoes;
    }

    public void setEspecificacoes(List<EspecificacaoEquipamento> especificacoes) {
        this.especificacoes = especificacoes;
    }

    public List<ImagemEquipamento> getImagens() {
        return imagens;
    }

    public void setImagens(List<ImagemEquipamento> imagens) {
        this.imagens = imagens;
    }

    public List<UnidadeEquipamento> getUnidades() {
        return unidades;
    }

    public void setUnidades(List<UnidadeEquipamento> unidades) {
        this.unidades = unidades;
    }
}
