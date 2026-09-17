package com.locaobra.dto.response;

import com.locaobra.entity.Cliente;
import com.locaobra.entity.Endereco;

import java.util.List;
import java.util.stream.Collectors;

// Perfil do cliente logado — usado pra pré-preencher o checkout de aluguel
// (endereços já cadastrados, nome, etc.) sem o front precisar confiar em
// query params soltos.
public class PerfilClienteResponse {

    private Long id;
    private String nome;
    private String cpfCnpj;
    private String telefone;
    private List<EnderecoResponse> enderecos;

    public static PerfilClienteResponse from(Cliente c, List<Endereco> enderecos) {
        PerfilClienteResponse r = new PerfilClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cpfCnpj = c.getCpfCnpj();
        r.telefone = c.getTelefone();
        r.enderecos = enderecos.stream().map(EnderecoResponse::from).collect(Collectors.toList());
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getTelefone() { return telefone; }
    public List<EnderecoResponse> getEnderecos() { return enderecos; }
}
