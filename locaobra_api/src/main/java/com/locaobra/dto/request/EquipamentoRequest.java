package com.locaobra.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class EquipamentoRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String descricao;

    @NotBlank(message = "Categoria é obrigatória")
    private String categoria;

    @NotNull(message = "Valor da diária é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor da diária deve ser maior que zero")
    private BigDecimal valorDiaria;

    private List<String> imagens;
    private Map<String, String> especificacoes;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getValorDiaria() { return valorDiaria; }
    public void setValorDiaria(BigDecimal valorDiaria) { this.valorDiaria = valorDiaria; }

    public List<String> getImagens() { return imagens; }
    public void setImagens(List<String> imagens) { this.imagens = imagens; }

    public Map<String, String> getEspecificacoes() { return especificacoes; }
    public void setEspecificacoes(Map<String, String> especificacoes) { this.especificacoes = especificacoes; }
}
