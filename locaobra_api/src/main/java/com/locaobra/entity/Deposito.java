package com.locaobra.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Local físico (galpão/pátio) onde ficam guardadas as unidades de equipamento
// e onde os funcionários de logística (conferente, entregador etc.) atuam.
// Vínculo é sempre com a UNIDADE (patrimônio/série), nunca com o modelo de
// Equipamento — o mesmo modelo pode ter unidades espalhadas em depósitos
// diferentes.
@Entity
@Table(name = "depositos", indexes = {
        @Index(name = "idx_depositos_endereco", columnList = "endereco_id")
})
public class Deposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nome;

    // Endereço fixo do depósito — armazenado numa linha própria da tabela
    // enderecos (sem vínculo de cliente) e referenciado por FK aqui.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
