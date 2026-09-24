package com.locaobra.dto.response;

import com.locaobra.enums.TipoVeiculo;

import java.math.BigDecimal;

// Resposta do POST /api/pedidos/estimar-frete. Tudo aqui é ESTIMATIVA — o
// valor final do frete é recalculado pelo consultor na confirmação do pedido,
// com o(s) depósito(s) real(is) de onde os itens vão sair.
public class FreteEstimativaResponse {

    private BigDecimal valorFrete;
    private Integer distanciaKm;
    private Integer prazoEstimadoDias;
    private BigDecimal pesoTotalKg;
    private BigDecimal pesoCubadoKg;
    private TipoVeiculo veiculoSugerido;

    public BigDecimal getValorFrete() { return valorFrete; }
    public void setValorFrete(BigDecimal valorFrete) { this.valorFrete = valorFrete; }

    public Integer getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Integer distanciaKm) { this.distanciaKm = distanciaKm; }

    public Integer getPrazoEstimadoDias() { return prazoEstimadoDias; }
    public void setPrazoEstimadoDias(Integer prazoEstimadoDias) { this.prazoEstimadoDias = prazoEstimadoDias; }

    public BigDecimal getPesoTotalKg() { return pesoTotalKg; }
    public void setPesoTotalKg(BigDecimal pesoTotalKg) { this.pesoTotalKg = pesoTotalKg; }

    public BigDecimal getPesoCubadoKg() { return pesoCubadoKg; }
    public void setPesoCubadoKg(BigDecimal pesoCubadoKg) { this.pesoCubadoKg = pesoCubadoKg; }

    public TipoVeiculo getVeiculoSugerido() { return veiculoSugerido; }
    public void setVeiculoSugerido(TipoVeiculo veiculoSugerido) { this.veiculoSugerido = veiculoSugerido; }
}