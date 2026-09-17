package com.locaobra.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Endereço salvo de um cliente (ex.: "Casa", "Obra Centro"). Um cliente pode
// ter vários. Também é reaproveitado, sem o vínculo de cliente, como o
// endereço fixo de um Depósito (ver Deposito.endereco) — nesse caso
// apelido/principal simplesmente ficam nulos/false.
@Entity
@Table(name = "enderecos", indexes = {
        @Index(name = "idx_enderecos_cliente", columnList = "cliente_id")
})
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Rótulo dado pelo próprio cliente, ex.: "Casa", "Obra Centro". Não se
    // aplica ao endereço de um Depósito.
    @Column(length = 60)
    private String apelido;

    @Column(length = 15)
    private String cep;

    @Column(length = 150)
    private String rua;

    @Column(length = 10)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    private String estado;

    // Endereço padrão do cliente — usado pra pré-preencher o checkout
    // quando ele tem mais de um salvo.
    @Column(nullable = false)
    private Boolean principal = false;

    // Dono do endereço. Nulo pro endereço "avulso" de um Depósito.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Boolean getPrincipal() { return principal; }
    public void setPrincipal(Boolean principal) { this.principal = principal; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
