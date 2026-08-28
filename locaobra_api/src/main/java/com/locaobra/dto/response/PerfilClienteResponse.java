package com.locaobra.dto.response;

import com.locaobra.entity.Cliente;
import com.locaobra.entity.Endereco;
import com.locaobra.enums.SituacaoCredito;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

// Perfil do cliente logado — usado pra pré-preencher o checkout de aluguel
// (endereços já cadastrados, nome, etc.) sem o front precisar confiar em
// query params soltos. Mostra a situação de crédito (pra explicar no
// checkout por que um pedido pode ser barrado), mas NUNCA as observações
// internas do analista — isso é só pra equipe, ver ClienteResponse.
public class PerfilClienteResponse {

    private Long id;
    private String nome;
    private String cpfCnpj;
    private String telefone;
    private List<EnderecoResponse> enderecos;

    private SituacaoCredito situacaoCredito;
    private BigDecimal limiteCredito;
    private BigDecimal creditoUtilizado;
    private BigDecimal creditoDisponivel;

    public static PerfilClienteResponse from(Cliente c, List<Endereco> enderecos, BigDecimal creditoUtilizado) {
        PerfilClienteResponse r = new PerfilClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cpfCnpj = c.getCpfCnpj();
        r.telefone = c.getTelefone();
        r.enderecos = enderecos.stream().map(EnderecoResponse::from).collect(Collectors.toList());

        r.situacaoCredito = c.getSituacaoCredito();
        r.limiteCredito = c.getLimiteCredito();
        r.creditoUtilizado = creditoUtilizado != null ? creditoUtilizado : BigDecimal.ZERO;
        r.creditoDisponivel = c.getLimiteCredito() != null ? c.getLimiteCredito().subtract(r.creditoUtilizado) : null;
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getTelefone() { return telefone; }
    public List<EnderecoResponse> getEnderecos() { return enderecos; }
    public SituacaoCredito getSituacaoCredito() { return situacaoCredito; }
    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public BigDecimal getCreditoUtilizado() { return creditoUtilizado; }
    public BigDecimal getCreditoDisponivel() { return creditoDisponivel; }
}
