package com.locaobra.dto.response;

import com.locaobra.entity.Cliente;
import com.locaobra.entity.Endereco;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClienteResponse {

    private Long id;
    private String nome;
    private String cpfCnpj;
    private String telefone;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private List<EnderecoResponse> enderecos = new ArrayList<>();

    public static ClienteResponse from(Cliente c) {
        return from(c, java.util.Collections.emptyList());
    }

    public static ClienteResponse from(Cliente c, List<Endereco> enderecos) {
        ClienteResponse r = new ClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cpfCnpj = c.getCpfCnpj();
        r.telefone = c.getTelefone();
        r.ativo = c.getAtivo();
        r.criadoEm = c.getCriadoEm();
        r.enderecos = enderecos.stream().map(EnderecoResponse::from).collect(Collectors.toList());
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getTelefone() { return telefone; }
    public Boolean getAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<EnderecoResponse> getEnderecos() { return enderecos; }
}
