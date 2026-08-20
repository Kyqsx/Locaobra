package com.locaobra.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ClienteRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "CPF/CNPJ é obrigatório")
    private String cpfCnpj;

    @NotBlank(message = "Telefone é obrigatório")
    private String telefone;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
