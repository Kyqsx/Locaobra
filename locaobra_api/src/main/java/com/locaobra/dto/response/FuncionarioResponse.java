package com.locaobra.dto.response;

import com.locaobra.entity.Funcionario;

import java.time.LocalDateTime;

public class FuncionarioResponse {

    private Long id;
    private String matricula;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private LocalDateTime dataNascimento;
    private Long cargoId;
    private String cargoNome;
    private Long departamentoId;
    private String departamentoNome;
    private Long depositoId;
    private String depositoNome;
    private Double salario;
    private LocalDateTime dataAdmissao;
    private LocalDateTime dataDemissao;
    private Boolean status;

    public static FuncionarioResponse from(Funcionario f) {
        FuncionarioResponse r = new FuncionarioResponse();
        r.id = f.getId();
        r.matricula = f.getMatricula();
        r.nome = f.getNome();
        r.cpf = f.getCpf();
        r.telefone = f.getTelefone();
        r.dataNascimento = f.getDataNascimento();
        if (f.getCargo() != null) {
            r.cargoId = f.getCargo().getId();
            r.cargoNome = f.getCargo().getNome();
        }
        if (f.getDepartamento() != null) {
            r.departamentoId = f.getDepartamento().getId();
            r.departamentoNome = f.getDepartamento().getNome();
        }
        if (f.getDeposito() != null) {
            r.depositoId = f.getDeposito().getId();
            r.depositoNome = f.getDeposito().getNome();
        }
        r.salario = f.getSalario();
        r.dataAdmissao = f.getDataAdmissao();
        r.dataDemissao = f.getDataDemissao();
        r.status = f.getStatus();
        return r;
    }

    public Long getId() { return id; }
    public String getMatricula() { return matricula; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getDataNascimento() { return dataNascimento; }
    public Long getCargoId() { return cargoId; }
    public String getCargoNome() { return cargoNome; }
    public Long getDepartamentoId() { return departamentoId; }
    public String getDepartamentoNome() { return departamentoNome; }
    public Long getDepositoId() { return depositoId; }
    public String getDepositoNome() { return depositoNome; }
    public Double getSalario() { return salario; }
    public LocalDateTime getDataAdmissao() { return dataAdmissao; }
    public LocalDateTime getDataDemissao() { return dataDemissao; }
    public Boolean getStatus() { return status; }
}