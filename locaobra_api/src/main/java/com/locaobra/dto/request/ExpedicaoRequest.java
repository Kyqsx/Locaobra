package com.locaobra.dto.request;

import com.locaobra.enums.TipoExpedicao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpedicaoRequest {

    private TipoExpedicao tipo;
    private Long clienteId;
    private Long motoristaId;
    // Preenchido quando a expedição é gerada pelo Conferente a partir de um
    // pedido APROVADO (fila-conferente). Só se aplica a tipo = ENTREGA — o
    // service usa esse id pra herdar cliente/endereço do pedido quando não
    // vierem explícitos no request, e pra vincular a expedição a ele.
    private Long pedidoId;
    // Obrigatório quando tipo = COLETA: id da expedição de ENTREGA (CONCLUIDO)
    // que está sendo buscada. Cliente, endereço e itens são copiados dela
    // automaticamente pelo service — não precisa (e não deve) mandar itens
    // manualmente nesse caso.
    private Long entregaOrigemId;
    // Até 3 nomes de quem pode receber o equipamento. Só usado quando
    // tipo = ENTREGA (COLETA herda automaticamente da entrega de origem).
    private java.util.List<String> nomesAutorizados;
    private String placaVeiculo;
    private LocalDate dataProgramada;
    private String horarioProgramado;
    private String enderecoEntrega;
    private String observacoes;
    private List<ItemExpedicaoRequest> itens = new ArrayList<>();

    public TipoExpedicao getTipo() { return tipo; }
    public void setTipo(TipoExpedicao tipo) { this.tipo = tipo; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public Long getEntregaOrigemId() { return entregaOrigemId; }
    public void setEntregaOrigemId(Long entregaOrigemId) { this.entregaOrigemId = entregaOrigemId; }

    public java.util.List<String> getNomesAutorizados() { return nomesAutorizados; }
    public void setNomesAutorizados(java.util.List<String> nomesAutorizados) { this.nomesAutorizados = nomesAutorizados; }

    public String getPlacaVeiculo() { return placaVeiculo; }
    public void setPlacaVeiculo(String placaVeiculo) { this.placaVeiculo = placaVeiculo; }

    public LocalDate getDataProgramada() { return dataProgramada; }
    public void setDataProgramada(LocalDate dataProgramada) { this.dataProgramada = dataProgramada; }

    public String getHorarioProgramado() { return horarioProgramado; }
    public void setHorarioProgramado(String horarioProgramado) { this.horarioProgramado = horarioProgramado; }

    public String getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(String enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public List<ItemExpedicaoRequest> getItens() { return itens; }
    public void setItens(List<ItemExpedicaoRequest> itens) { this.itens = itens; }
}