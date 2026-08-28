package com.locaobra.dto.response;

import com.locaobra.entity.Cliente;
import com.locaobra.entity.Endereco;
import com.locaobra.enums.SituacaoCredito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    // ===================== CRÉDITO =====================
    private SituacaoCredito situacaoCredito;
    private BigDecimal limiteCredito;
    // Soma dos pedidos em aberto do cliente (SOLICITADO/CONFIRMADO/APROVADO),
    // calculada na hora — ver ClienteService.calcularCreditoUtilizado().
    private BigDecimal creditoUtilizado;
    private BigDecimal creditoDisponivel;
    private String observacoesCredito;

    public static ClienteResponse from(Cliente c) {
        return from(c, Collections.emptyList(), BigDecimal.ZERO);
    }

    public static ClienteResponse from(Cliente c, List<Endereco> enderecos) {
        return from(c, enderecos, BigDecimal.ZERO);
    }

    public static ClienteResponse from(Cliente c, List<Endereco> enderecos, BigDecimal creditoUtilizado) {
        ClienteResponse r = new ClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cpfCnpj = c.getCpfCnpj();
        r.telefone = c.getTelefone();
        r.ativo = c.getAtivo();
        r.criadoEm = c.getCriadoEm();
        r.enderecos = enderecos.stream().map(EnderecoResponse::from).collect(Collectors.toList());

        r.situacaoCredito = c.getSituacaoCredito();
        r.limiteCredito = c.getLimiteCredito();
        r.observacoesCredito = c.getObservacoesCredito();
        r.creditoUtilizado = creditoUtilizado != null ? creditoUtilizado : BigDecimal.ZERO;
        r.creditoDisponivel = c.getLimiteCredito() != null ? c.getLimiteCredito().subtract(r.creditoUtilizado) : null;
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getTelefone() { return telefone; }
    public Boolean getAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<EnderecoResponse> getEnderecos() { return enderecos; }
    public SituacaoCredito getSituacaoCredito() { return situacaoCredito; }
    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public BigDecimal getCreditoUtilizado() { return creditoUtilizado; }
    public BigDecimal getCreditoDisponivel() { return creditoDisponivel; }
    public String getObservacoesCredito() { return observacoesCredito; }
}
