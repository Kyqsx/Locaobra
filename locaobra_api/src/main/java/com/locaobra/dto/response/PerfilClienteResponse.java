package com.locaobra.dto.response;

import com.locaobra.entity.Cliente;

// Perfil do cliente logado — usado pra pré-preencher o checkout de aluguel
// (endereço já cadastrado, nome, etc.) sem o front precisar confiar em
// query params soltos.
public class PerfilClienteResponse {

    private Long id;
    private String nome;
    private String cpfCnpj;
    private String telefone;
    private String enderecoFormatado;
    private Long idEndereco;

    public static PerfilClienteResponse from(Cliente c, String enderecoFormatado, Long idEndereco) {
        PerfilClienteResponse r = new PerfilClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cpfCnpj = c.getCpfCnpj();
        r.telefone = c.getTelefone();
        r.enderecoFormatado = enderecoFormatado;
        r.idEndereco = idEndereco;
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getTelefone() { return telefone; }
    public String getEnderecoFormatado() { return enderecoFormatado; }
    public Long getIdEndereco() { return idEndereco; }
}
