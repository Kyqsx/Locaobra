package com.locaobra.dto.response;

import java.math.BigDecimal;

public class DashboardResponse {

    private Long totalClientes;
    private Long totalEquipamentos;
    private Long equipamentosDisponiveis;
    private Long equipamentosAlugados;
    private Long equipamentosEmManutencao;
    private Long alugueisAtivos;
    private Long alugueisFinalizados;
    private Long alugueisCancelados;
    private BigDecimal receitaTotal;

    public DashboardResponse() {}

    public Long getTotalClientes() { return totalClientes; }
    public void setTotalClientes(Long totalClientes) { this.totalClientes = totalClientes; }

    public Long getTotalEquipamentos() { return totalEquipamentos; }
    public void setTotalEquipamentos(Long totalEquipamentos) { this.totalEquipamentos = totalEquipamentos; }

    public Long getEquipamentosDisponiveis() { return equipamentosDisponiveis; }
    public void setEquipamentosDisponiveis(Long equipamentosDisponiveis) { this.equipamentosDisponiveis = equipamentosDisponiveis; }

    public Long getEquipamentosAlugados() { return equipamentosAlugados; }
    public void setEquipamentosAlugados(Long equipamentosAlugados) { this.equipamentosAlugados = equipamentosAlugados; }

    public Long getEquipamentosEmManutencao() { return equipamentosEmManutencao; }
    public void setEquipamentosEmManutencao(Long equipamentosEmManutencao) { this.equipamentosEmManutencao = equipamentosEmManutencao; }

    public Long getAlugueisAtivos() { return alugueisAtivos; }
    public void setAlugueisAtivos(Long alugueisAtivos) { this.alugueisAtivos = alugueisAtivos; }

    public Long getAlugueisFinalizados() { return alugueisFinalizados; }
    public void setAlugueisFinalizados(Long alugueisFinalizados) { this.alugueisFinalizados = alugueisFinalizados; }

    public Long getAlugueisCancelados() { return alugueisCancelados; }
    public void setAlugueisCancelados(Long alugueisCancelados) { this.alugueisCancelados = alugueisCancelados; }

    public BigDecimal getReceitaTotal() { return receitaTotal; }
    public void setReceitaTotal(BigDecimal receitaTotal) { this.receitaTotal = receitaTotal; }
}
