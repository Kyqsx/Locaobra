package com.locaobra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "imagens_equipamento")
public class ImagemEquipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    @Column(nullable = false)
    private String url;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    // Ponto focal do recorte 1:1 (0–100, centro por padrão). A imagem original
    // nunca é cortada — só o enquadramento de exibição usa essas coordenadas.
    @Column(name = "foco_x", nullable = false)
    private Integer focoX = 50;

    @Column(name = "foco_y", nullable = false)
    private Integer focoY = 50;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public Integer getFocoX() {
        return focoX;
    }

    public void setFocoX(Integer focoX) {
        this.focoX = focoX;
    }

    public Integer getFocoY() {
        return focoY;
    }

    public void setFocoY(Integer focoY) {
        this.focoY = focoY;
    }
}
