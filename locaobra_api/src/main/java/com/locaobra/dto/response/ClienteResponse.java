package com.locaobra.dto.response;

import com.locaobra.entity.Cliente;
import java.time.LocalDateTime;

public class ClienteResponse {

    private Long id;
    private String nome;
    private String cpfCnpj;
    private String telefone;
    private Boolean ativo;
    private LocalDateTime criadoEm;

    public static ClienteResponse from(Cliente c) {
        ClienteResponse r = new ClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cpfCnpj = c.getCpfCnpj();
        r.telefone = c.getTelefone();
        r.ativo = c.getAtivo();
        r.criadoEm = c.getCriadoEm();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getTelefone() { return telefone; }
    public Boolean getAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
