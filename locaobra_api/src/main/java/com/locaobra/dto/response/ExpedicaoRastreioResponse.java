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

/**
 * Versão enxuta do ExpedicaoResponse pra tela de RASTREIO DO CLIENTE
 * (mobile). Só o que ajuda o cliente a acompanhar a própria entrega/coleta —
 * sem ids/campos internos de operação (motorista/depósito por id, documento
 * de quem recebeu, vistorias, etc.), que não dizem respeito a quem só quer
 * saber "cadê meu equipamento".
 */
public class ExpedicaoRastreioResponse {

    private Long id;
    private String codigo;
    private TipoExpedicao tipo;
    private StatusExpedicao status;
    private String pedidoCodigo;
    private LocalDate dataProgramada;
    private String horarioProgramado;
    private EnderecoResponse enderecoEntrega;
    private String motoristaNome;
    private String observacoes;
    private LocalDateTime criadoEm;
    private LocalDateTime checkoutEm;
    private LocalDateTime entregaConfirmadaEm;
    private String assinaturaEntrega;
    private String fotoEntrega;
    private String motivoCancelamento;
    private List<ItemRastreioResponse> itens;

    public static ExpedicaoRastreioResponse from(Expedicao e, List<ItemExpedicao> itens) {
        ExpedicaoRastreioResponse r = new ExpedicaoRastreioResponse();
        r.id = e.getId();
        r.codigo = e.getCodigo();
        r.tipo = e.getTipo();
        r.status = e.getStatus();
        r.pedidoCodigo = e.getPedido() != null ? e.getPedido().getCodigo() : null;
        r.dataProgramada = e.getDataProgramada();
        r.horarioProgramado = e.getHorarioProgramado();
        r.enderecoEntrega = EnderecoResponse.from(e.getEnderecoEntrega());
        r.motoristaNome = e.getMotorista() != null ? e.getMotorista().getNome() : null;
        r.observacoes = e.getObservacoes();
        r.criadoEm = e.getCriadoEm();
        r.checkoutEm = e.getCheckoutEm();
        r.entregaConfirmadaEm = e.getEntregaConfirmadaEm();
        r.assinaturaEntrega = e.getAssinaturaEntrega();
        r.fotoEntrega = e.getFotoEntrega();
        r.motivoCancelamento = e.getMotivoCancelamento();
        r.itens = itens != null
                ? itens.stream().map(ItemRastreioResponse::from).collect(Collectors.toList())
                : Collections.emptyList();
        return r;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public TipoExpedicao getTipo() { return tipo; }
    public StatusExpedicao getStatus() { return status; }
    public String getPedidoCodigo() { return pedidoCodigo; }
    public LocalDate getDataProgramada() { return dataProgramada; }
    public String getHorarioProgramado() { return horarioProgramado; }
    public EnderecoResponse getEnderecoEntrega() { return enderecoEntrega; }
    public String getMotoristaNome() { return motoristaNome; }
    public String getObservacoes() { return observacoes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getCheckoutEm() { return checkoutEm; }
    public LocalDateTime getEntregaConfirmadaEm() { return entregaConfirmadaEm; }
    public String getAssinaturaEntrega() { return assinaturaEntrega; }
    public String getFotoEntrega() { return fotoEntrega; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public List<ItemRastreioResponse> getItens() { return itens; }

    public static class ItemRastreioResponse {
        private String equipamentoNome;
        private Integer quantidade;

        public static ItemRastreioResponse from(ItemExpedicao i) {
            ItemRastreioResponse r = new ItemRastreioResponse();
            r.equipamentoNome = i.getEquipamento() != null ? i.getEquipamento().getNome() : null;
            r.quantidade = i.getQuantidade();
            return r;
        }

        public String getEquipamentoNome() { return equipamentoNome; }
        public Integer getQuantidade() { return quantidade; }
    }
}
