package com.locaobra.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class ClienteRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "CPF/CNPJ é obrigatório")
    private String cpfCnpj;

    @NotBlank(message = "Telefone é obrigatório")
    private String telefone;

    // Endereço(s) já preenchidos no cadastro, feito pelo funcionário/admin.
    // Opcional — um cliente pode ser cadastrado sem endereço e adicioná-lo
    // depois pelos endpoints de /enderecos.
    private List<EnderecoRequest> enderecos = new ArrayList<>();

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public List<EnderecoRequest> getEnderecos() { return enderecos; }
    public void setEnderecos(List<EnderecoRequest> enderecos) { this.enderecos = enderecos; }
}
