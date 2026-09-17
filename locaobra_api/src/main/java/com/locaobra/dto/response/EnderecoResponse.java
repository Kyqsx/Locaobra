package com.locaobra.dto.response;

import com.locaobra.entity.Endereco;

public class EnderecoResponse {

    private Long id;
    private String apelido;
    private String cep;
    private String rua;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private Boolean principal;
    private String formatado;

    public static EnderecoResponse from(Endereco e) {
        if (e == null) return null;
        EnderecoResponse r = new EnderecoResponse();
        r.id = e.getId();
        r.apelido = e.getApelido();
        r.cep = e.getCep();
        r.rua = e.getRua();
        r.numero = e.getNumero();
        r.complemento = e.getComplemento();
        r.bairro = e.getBairro();
        r.cidade = e.getCidade();
        r.estado = e.getEstado();
        r.principal = e.getPrincipal();
        r.formatado = formatar(e);
        return r;
    }

    // Linha única, pra exibição rápida em telas/relatórios que não precisam
    // dos campos separados.
    private static String formatar(Endereco e) {
        if (e == null) return null;
        StringBuilder sb = new StringBuilder();
        if (e.getRua() != null && !e.getRua().isBlank()) sb.append(e.getRua());
        if (e.getNumero() != null && !e.getNumero().isBlank()) sb.append(", ").append(e.getNumero());
        if (e.getComplemento() != null && !e.getComplemento().isBlank()) sb.append(" - ").append(e.getComplemento());
        if (e.getBairro() != null && !e.getBairro().isBlank()) sb.append(" - ").append(e.getBairro());
        if (e.getCidade() != null && !e.getCidade().isBlank()) sb.append(", ").append(e.getCidade());
        if (e.getEstado() != null && !e.getEstado().isBlank()) sb.append("/").append(e.getEstado());
        if (e.getCep() != null && !e.getCep().isBlank()) sb.append(" - CEP ").append(e.getCep());
        return sb.toString();
    }

    public Long getId() { return id; }
    public String getApelido() { return apelido; }
    public String getCep() { return cep; }
    public String getRua() { return rua; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
    public Boolean getPrincipal() { return principal; }
    public String getFormatado() { return formatado; }
}
