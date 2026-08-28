package com.locaobra.dto.response;

import com.locaobra.entity.Expedicao;
import com.locaobra.entity.ItemExpedicao;
import com.locaobra.enums.StatusExpedicao;
import com.locaobra.enums.TipoExpedicao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExpedicaoResponse {

    private Long id;
    private String codigo;
    private TipoExpedicao tipo;
    private StatusExpedicao status;
    private Long clienteId;
    private String clienteNome;
    private Long motoristaId;
    private String motoristaNome;
    private Long entregaOrigemId;
    private String entregaOrigemCodigo;
    private Long pedidoId;
    private String pedidoCodigo;
    private Long depositoOrigemId;
    private String depositoOrigemNome;
    private java.util.List<String> nomesAutorizados;
    private String placaVeiculo;
    private LocalDate dataProgramada;
    private String horarioProgramado;
    private EnderecoResponse enderecoEntrega;
    private String observacoes;
    private LocalDateTime checkoutEm;
    private LocalDateTime checkinEm;
    private String assinaturaCliente;
    private LocalDateTime entregaConfirmadaEm;
    private String assinaturaEntrega;
    private String fotoEntrega;
    private LocalDateTime criadoEm;
    private List<ItemExpedicaoResponse> itens;
    private List<VistoriaResponse> vistorias;

    public static ExpedicaoResponse from(Expedicao e) {
        ExpedicaoResponse r = new ExpedicaoResponse();
        r.id = e.getId();
        r.codigo = e.getCodigo();
        r.tipo = e.getTipo();
        r.status = e.getStatus();
        r.clienteId = e.getCliente() != null ? e.getCliente().getId() : null;
        r.clienteNome = e.getCliente() != null ? e.getCliente().getNome() : null;
        r.motoristaId = e.getMotorista() != null ? e.getMotorista().getId() : null;
        r.motoristaNome = e.getMotorista() != null ? e.getMotorista().getNome() : null;
        r.entregaOrigemId = e.getEntregaOrigem() != null ? e.getEntregaOrigem().getId() : null;
        r.entregaOrigemCodigo = e.getEntregaOrigem() != null ? e.getEntregaOrigem().getCodigo() : null;
        r.pedidoId = e.getPedido() != null ? e.getPedido().getId() : null;
        r.pedidoCodigo = e.getPedido() != null ? e.getPedido().getCodigo() : null;
        r.depositoOrigemId = e.getDepositoOrigem() != null ? e.getDepositoOrigem().getId() : null;
        r.depositoOrigemNome = e.getDepositoOrigem() != null ? e.getDepositoOrigem().getNome() : null;
        r.nomesAutorizados = java.util.stream.Stream.of(e.getNomeAutorizado1(), e.getNomeAutorizado2(), e.getNomeAutorizado3())
                .filter(n -> n != null && !n.isBlank())
                .collect(java.util.stream.Collectors.toList());
        r.placaVeiculo = e.getPlacaVeiculo();
        r.dataProgramada = e.getDataProgramada();
        r.horarioProgramado = e.getHorarioProgramado();
        r.enderecoEntrega = EnderecoResponse.from(e.getEnderecoEntrega());
        r.observacoes = e.getObservacoes();
        r.checkoutEm = e.getCheckoutEm();
        r.checkinEm = e.getCheckinEm();
        r.assinaturaCliente = e.getAssinaturaCliente();
        r.entregaConfirmadaEm = e.getEntregaConfirmadaEm();
        r.assinaturaEntrega = e.getAssinaturaEntrega();
        r.fotoEntrega = e.getFotoEntrega();
        r.criadoEm = e.getCriadoEm();
        // NÃO lemos e.getItens()/e.getVistorias() aqui de propósito: essas coleções são
        // mapeadas com orphanRemoval=true, e qualquer clear()/mutação nelas feita em outro
        // lugar do código apaga as linhas no banco. Use from(e, itens, vistorias) para
        // montar a resposta com dados buscados direto do repositório, sem tocar na entidade.
        r.itens = Collections.emptyList();
        r.vistorias = Collections.emptyList();
        return r;
    }

    // vistorias já vem como VistoriaResponse (com fotos já resolvidas) para evitar
    // tocar na coleção lazy/orphanRemoval de Vistoria.fotos por aqui.
    public static ExpedicaoResponse from(Expedicao e, List<ItemExpedicao> itens, List<VistoriaResponse> vistorias) {
        ExpedicaoResponse r = from(e);
        r.itens = itens != null
                ? itens.stream().map(ItemExpedicaoResponse::from).collect(Collectors.toList())
                : Collections.emptyList();
        r.vistorias = vistorias != null ? vistorias : Collections.emptyList();
        return r;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public TipoExpedicao getTipo() { return tipo; }
    public StatusExpedicao getStatus() { return status; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNome() { return clienteNome; }
    public Long getMotoristaId() { return motoristaId; }
    public String getMotoristaNome() { return motoristaNome; }
    public Long getEntregaOrigemId() { return entregaOrigemId; }
    public String getEntregaOrigemCodigo() { return entregaOrigemCodigo; }
    public Long getPedidoId() { return pedidoId; }
    public String getPedidoCodigo() { return pedidoCodigo; }
    public Long getDepositoOrigemId() { return depositoOrigemId; }
    public String getDepositoOrigemNome() { return depositoOrigemNome; }
    public java.util.List<String> getNomesAutorizados() { return nomesAutorizados; }
    public String getPlacaVeiculo() { return placaVeiculo; }
    public LocalDate getDataProgramada() { return dataProgramada; }
    public String getHorarioProgramado() { return horarioProgramado; }
    public EnderecoResponse getEnderecoEntrega() { return enderecoEntrega; }
    public String getObservacoes() { return observacoes; }
    public LocalDateTime getCheckoutEm() { return checkoutEm; }
    public LocalDateTime getCheckinEm() { return checkinEm; }
    public String getAssinaturaCliente() { return assinaturaCliente; }
    public LocalDateTime getEntregaConfirmadaEm() { return entregaConfirmadaEm; }
    public String getAssinaturaEntrega() { return assinaturaEntrega; }
    public String getFotoEntrega() { return fotoEntrega; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<ItemExpedicaoResponse> getItens() { return itens; }
    public List<VistoriaResponse> getVistorias() { return vistorias; }
}