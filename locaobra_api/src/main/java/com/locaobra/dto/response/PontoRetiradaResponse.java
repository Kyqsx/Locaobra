package com.locaobra.dto.response;

import com.locaobra.entity.Deposito;

// Ponto de retirada exibido pro cliente no carrinho (quando ele escolhe
// RETIRADA): só o essencial — nome e endereço do depósito. Sem contagens
// internas (unidades/funcionários) que a lista de depósitos do staff expõe.
public class PontoRetiradaResponse {

    private Long id;
    private String nome;
    private EnderecoResponse endereco;

    public static PontoRetiradaResponse from(Deposito d) {
        PontoRetiradaResponse r = new PontoRetiradaResponse();
        r.id = d.getId();
        r.nome = d.getNome();
        r.endereco = EnderecoResponse.from(d.getEndereco());
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public EnderecoResponse getEndereco() { return endereco; }
}