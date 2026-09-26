package com.locaobra.dto.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.locaobra.enums.FormaPagamento;
import com.locaobra.enums.TipoEntrega;

// Usado pelo CLIENTE para solicitar um orçamento de aluguel pelo catálogo.
// O clienteId NÃO vem daqui — é resolvido no service a partir do usuário
// logado (token JWT), pra ninguém pedir em nome de outro cliente.
public class PedidoRequest {

    private LocalDate dataInicio;
    private LocalDate dataFim;

    // Duas formas de informar o endereço de entrega — o service usa
    // enderecoId quando presente (endereço já salvo, tirado do carrinho/
    // checkout) e cai pra enderecoEntrega (digitado na hora) caso contrário.
    // Ignorados quando tipoEntrega = RETIRADA (cliente busca no depósito).
    private Long enderecoId;
    private EnderecoRequest enderecoEntrega;

    // ENTREGA (padrão) ou RETIRADA. Na retirada o endereço é dispensado e o
    // frete sai zero.
    private TipoEntrega tipoEntrega;

    private String observacoesCliente;
    private List<ItemPedidoRequest> itens = new ArrayList<>();

    // Opcional: preenchido pela tela de pagamento simulado (web/mobile) logo
    // após o cliente "pagar". Se vier, o pedido já nasce com statusPagamento
    // PAGO; se não vier (app antigo, sem a tela nova), fica PENDENTE — sem
    // quebrar compatibilidade com quem ainda manda pedido direto do carrinho.
    private FormaPagamento formaPagamento;

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public Long getEnderecoId() { return enderecoId; }
    public void setEnderecoId(Long enderecoId) { this.enderecoId = enderecoId; }

    public EnderecoRequest getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(EnderecoRequest enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }

    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(TipoEntrega tipoEntrega) { this.tipoEntrega = tipoEntrega; }

    public String getObservacoesCliente() { return observacoesCliente; }
    public void setObservacoesCliente(String observacoesCliente) { this.observacoesCliente = observacoesCliente; }

    public List<ItemPedidoRequest> getItens() { return itens; }
    public void setItens(List<ItemPedidoRequest> itens) { this.itens = itens; }

    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
}
