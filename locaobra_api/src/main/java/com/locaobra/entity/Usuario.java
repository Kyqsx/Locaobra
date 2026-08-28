package com.locaobra.entity;

import com.locaobra.enums.TipoUsuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUsuario tipo;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "id_cliente")
    private Long idCliente;

    @Column(name = "id_funcionario")
    private Long idFuncionario;

    @Column(name = "id_endereco")
    private Long idEndereco;

    // ===================== VERIFICAÇÃO DE EMAIL =====================
    // Legados (linhas antigas) ficam com null — e o login trata null como verificado.
    @Column(name = "email_verificado")
    private Boolean emailVerificado;

    @Column(name = "token_verificacao", length = 64)
    private String tokenVerificacao;

    @Column(name = "token_verificacao_expira_em")
    private LocalDateTime tokenVerificacaoExpiraEm;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public TipoUsuario getTipo() {
        return tipo;
    }

    public void setTipo(TipoUsuario tipo) {
        this.tipo = tipo;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Long getIdFuncionario() {
        return idFuncionario;
    }

    public void setIdFuncionario(Long idFuncionario) {
        this.idFuncionario = idFuncionario;
    }

    public Long getIdEndereco() {
        return idEndereco;
    }

    public void setIdEndereco(Long idEndereco) {
        this.idEndereco = idEndereco;
    }

    public Boolean getEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public String getTokenVerificacao() {
        return tokenVerificacao;
    }

    public void setTokenVerificacao(String tokenVerificacao) {
        this.tokenVerificacao = tokenVerificacao;
    }

    public LocalDateTime getTokenVerificacaoExpiraEm() {
        return tokenVerificacaoExpiraEm;
    }

    public void setTokenVerificacaoExpiraEm(LocalDateTime tokenVerificacaoExpiraEm) {
        this.tokenVerificacaoExpiraEm = tokenVerificacaoExpiraEm;
    }
}
