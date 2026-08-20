package com.locaobra.dto.response;

import com.locaobra.entity.Equipamento;
import com.locaobra.enums.StatusUnidade;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EquipamentoResponse {

    private Long id;
    private String nome;
    private String descricao;
    private String categoria;
    private BigDecimal valorDiaria;
    private String status;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private Map<String, String> especificacoes;
    private List<String> imagens;
    private Integer quantidadeTotal;
    private Integer quantidadeDisponivel;
    private List<UnidadeEquipamentoResponse> unidades;

    public static EquipamentoResponse from(Equipamento e) {
        EquipamentoResponse r = new EquipamentoResponse();
        r.id = e.getId();
        r.nome = e.getNome();
        r.descricao = e.getDescricao();
        r.categoria = e.getCategoria();
        r.valorDiaria = e.getValorDiaria();
        r.status = e.getStatus();
        r.criadoEm = e.getCriadoEm();
        r.atualizadoEm = e.getAtualizadoEm();

        if (e.getEspecificacoes() != null && !e.getEspecificacoes().isEmpty()) {
            r.especificacoes = e.getEspecificacoes().stream()
                    .collect(Collectors.toMap(
                            spec -> spec.getChave(),
                            spec -> spec.getValor()
                    ));
        }

        if (e.getImagens() != null && !e.getImagens().isEmpty()) {
            r.imagens = e.getImagens().stream()
                    .map(img -> img.getUrl())
                    .collect(Collectors.toList());
        }

        List<com.locaobra.entity.UnidadeEquipamento> unidades = e.getUnidades();
        if (unidades == null) {
            unidades = Collections.emptyList();
        }
        r.quantidadeTotal = unidades.size();
        r.quantidadeDisponivel = (int) unidades.stream()
                .filter(u -> u.getStatus() == StatusUnidade.DISPONIVEL)
                .count();
        r.unidades = unidades.stream()
                .map(UnidadeEquipamentoResponse::from)
                .collect(Collectors.toList());

        return r;
    }

    // Getters
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getValorDiaria() { return valorDiaria; }
    public String getStatus() { return status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Map<String, String> getEspecificacoes() { return especificacoes; }
    public List<String> getImagens() { return imagens; }
    public Integer getQuantidadeTotal() { return quantidadeTotal; }
    public Integer getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public List<UnidadeEquipamentoResponse> getUnidades() { return unidades; }
}
