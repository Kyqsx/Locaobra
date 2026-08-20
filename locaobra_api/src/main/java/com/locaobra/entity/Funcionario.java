package com.locaobra.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "funcionarios")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula", nullable = false, unique = true, length = 30)
    private String matricula;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf", nullable = false, unique = true, length = 20)
    private String cpf;

    @Column(name = "telefone", length = 20, nullable = true, unique = true)
    private String telefone;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDateTime dataNascimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id")
    private DepartamentoEntity departamento;

    @Column(name = "salario", nullable = false)
    private Double salario;

    @Column(name = "data_admissao", nullable = false)
    private LocalDateTime dataAdmissao;

    @Column(name = "data_demissao", nullable = true)
    private LocalDateTime dataDemissao;

    @Column(name = "status", nullable = false)
    private Boolean status;

    //Getters e Setters

    public Long getId() {
        return id;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public LocalDateTime getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDateTime dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public DepartamentoEntity getDepartamento() {
        return departamento;
    }

    public void setDepartamento(DepartamentoEntity departamento) {
        this.departamento = departamento;
    }

    public Double getSalario() {
        return salario;
    }

    public void setSalario(Double salario) {
        this.salario = salario;
    }

    public LocalDateTime getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(LocalDateTime dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public LocalDateTime getDataDemissao() {
        return dataDemissao;
    }

    public void setDataDemissao(LocalDateTime dataDemissao) {
        this.dataDemissao = dataDemissao;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}