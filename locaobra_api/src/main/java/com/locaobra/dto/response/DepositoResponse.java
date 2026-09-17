package com.locaobra.dto.response;

import com.locaobra.entity.Deposito;

import java.time.LocalDateTime;

public class DepositoResponse {

    private Long id;
    private String nome;
    private EnderecoResponse endereco;
    private String descricao;
    private Boolean ativo;
    // Contagens de apoio pra tela de listagem (quantas unidades de equipamento
    // e quantos funcionários estão vinculados a este depósito hoje). Preenchidas
    // pelo DepositoService, não vêm direto da entidade.
    private Long quantidadeUnidades;
    private Long quantidadeFuncionarios;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static DepositoResponse from(Deposito d) {
        DepositoResponse r = new DepositoResponse();
        r.id = d.getId();
        r.nome = d.getNome();
        r.endereco = EnderecoResponse.from(d.getEndereco());
        r.descricao = d.getDescricao();
        r.ativo = d.getAtivo();
        r.criadoEm = d.getCriadoEm();
        r.atualizadoEm = d.getAtualizadoEm();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public EnderecoResponse getEndereco() { return endereco; }
    public String getDescricao() { return descricao; }
    public Boolean getAtivo() { return ativo; }
    public Long getQuantidadeUnidades() { return quantidadeUnidades; }
    public void setQuantidadeUnidades(Long quantidadeUnidades) { this.quantidadeUnidades = quantidadeUnidades; }
    public Long getQuantidadeFuncionarios() { return quantidadeFuncionarios; }
    public void setQuantidadeFuncionarios(Long quantidadeFuncionarios) { this.quantidadeFuncionarios = quantidadeFuncionarios; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
