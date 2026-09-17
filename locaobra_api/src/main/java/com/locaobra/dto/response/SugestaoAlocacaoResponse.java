package com.locaobra.dto.response;

import java.util.ArrayList;
import java.util.List;

// Resposta do motor de sugestão de depósito(s) pra um pedido — usado pelo
// consultor na hora de confirmar. Ver PedidoService.sugerirAlocacaoDepositos().
public class SugestaoAlocacaoResponse {

    private boolean atendeUmDeposito;
    private Long depositoUnicoId;
    private String depositoUnicoNome;
    private List<GrupoDeposito> grupos = new ArrayList<>();
    private List<ItemNaoAtendido> itensNaoAtendidos = new ArrayList<>();

    public boolean isAtendeUmDeposito() { return atendeUmDeposito; }
    public void setAtendeUmDeposito(boolean atendeUmDeposito) { this.atendeUmDeposito = atendeUmDeposito; }

    public Long getDepositoUnicoId() { return depositoUnicoId; }
    public void setDepositoUnicoId(Long depositoUnicoId) { this.depositoUnicoId = depositoUnicoId; }

    public String getDepositoUnicoNome() { return depositoUnicoNome; }
    public void setDepositoUnicoNome(String depositoUnicoNome) { this.depositoUnicoNome = depositoUnicoNome; }

    public List<GrupoDeposito> getGrupos() { return grupos; }
    public void setGrupos(List<GrupoDeposito> grupos) { this.grupos = grupos; }

    public List<ItemNaoAtendido> getItensNaoAtendidos() { return itensNaoAtendidos; }
    public void setItensNaoAtendidos(List<ItemNaoAtendido> itensNaoAtendidos) { this.itensNaoAtendidos = itensNaoAtendidos; }

    // Um depósito e os itens do pedido que ele consegue atender totalmente.
    public static class GrupoDeposito {
        private Long depositoId;
        private String depositoNome;
        private List<ItemAlocado> itens = new ArrayList<>();

        public Long getDepositoId() { return depositoId; }
        public void setDepositoId(Long depositoId) { this.depositoId = depositoId; }

        public String getDepositoNome() { return depositoNome; }
        public void setDepositoNome(String depositoNome) { this.depositoNome = depositoNome; }

        public List<ItemAlocado> getItens() { return itens; }
        public void setItens(List<ItemAlocado> itens) { this.itens = itens; }
    }

    // Um item do pedido dentro de um grupo, com quanto tem disponível ali.
    public static class ItemAlocado {
        private Long itemPedidoId;
        private Long equipamentoId;
        private String equipamentoNome;
        private Integer quantidade;
        private long disponivelNoDeposito;

        public Long getItemPedidoId() { return itemPedidoId; }
        public void setItemPedidoId(Long itemPedidoId) { this.itemPedidoId = itemPedidoId; }

        public Long getEquipamentoId() { return equipamentoId; }
        public void setEquipamentoId(Long equipamentoId) { this.equipamentoId = equipamentoId; }

        public String getEquipamentoNome() { return equipamentoNome; }
        public void setEquipamentoNome(String equipamentoNome) { this.equipamentoNome = equipamentoNome; }

        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

        public long getDisponivelNoDeposito() { return disponivelNoDeposito; }
        public void setDisponivelNoDeposito(long disponivelNoDeposito) { this.disponivelNoDeposito = disponivelNoDeposito; }
    }

    // Item que nenhum depósito sozinho consegue atender por completo — o
    // consultor precisa decidir manualmente (recusar, dividir a quantidade
    // entre depósitos manualmente, ou aguardar reposição de estoque).
    public static class ItemNaoAtendido {
        private Long itemPedidoId;
        private String equipamentoNome;
        private Integer quantidade;
        private long maiorDisponibilidadeEncontrada;
        private Long depositoComMaisDisponibilidadeId;
        private String depositoComMaisDisponibilidadeNome;

        public Long getItemPedidoId() { return itemPedidoId; }
        public void setItemPedidoId(Long itemPedidoId) { this.itemPedidoId = itemPedidoId; }

        public String getEquipamentoNome() { return equipamentoNome; }
        public void setEquipamentoNome(String equipamentoNome) { this.equipamentoNome = equipamentoNome; }

        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

        public long getMaiorDisponibilidadeEncontrada() { return maiorDisponibilidadeEncontrada; }
        public void setMaiorDisponibilidadeEncontrada(long maiorDisponibilidadeEncontrada) { this.maiorDisponibilidadeEncontrada = maiorDisponibilidadeEncontrada; }

        public Long getDepositoComMaisDisponibilidadeId() { return depositoComMaisDisponibilidadeId; }
        public void setDepositoComMaisDisponibilidadeId(Long depositoComMaisDisponibilidadeId) { this.depositoComMaisDisponibilidadeId = depositoComMaisDisponibilidadeId; }

        public String getDepositoComMaisDisponibilidadeNome() { return depositoComMaisDisponibilidadeNome; }
        public void setDepositoComMaisDisponibilidadeNome(String depositoComMaisDisponibilidadeNome) { this.depositoComMaisDisponibilidadeNome = depositoComMaisDisponibilidadeNome; }
    }
}
