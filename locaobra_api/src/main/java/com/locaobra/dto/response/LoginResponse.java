package com.locaobra.dto.response;

import com.locaobra.enums.TipoUsuario;

public class LoginResponse {

    private String token;
    private String login;
    private String nome;
    private TipoUsuario tipo;

    public LoginResponse(String token, String login, String nome, TipoUsuario tipo) {
        this.token = token;
        this.login = login;
        this.nome = nome;
        this.tipo = tipo;
    }

    public String getToken() { return token; }
    public String getLogin() { return login; }
    public String getNome() { return nome; }
    public TipoUsuario getTipo() { return tipo; }
}
