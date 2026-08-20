package com.locaobra.dto.response;

import com.locaobra.entity.Usuario;
import com.locaobra.enums.TipoUsuario;
import java.time.LocalDateTime;

public class UsuarioResponse {

    private Long id;
    private String nome;
    private String email;
    private TipoUsuario tipo;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private Long idFuncionario;

    public static UsuarioResponse from(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.id = u.getId();
        r.nome = u.getNome();
        r.email = u.getEmail();
        r.tipo = u.getTipo();
        r.ativo = u.getAtivo();
        r.criadoEm = u.getCriadoEm();
        r.idFuncionario = u.getIdFuncionario();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public TipoUsuario getTipo() { return tipo; }
    public Boolean getAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Long getIdFuncionario() { return idFuncionario; }
}
