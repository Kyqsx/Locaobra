package com.locaobra.entity;

import com.locaobra.enums.StatusExpedicao;
import com.locaobra.enums.TipoExpedicao;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expedicoes", indexes = {
        @Index(name = "idx_expedicoes_endereco", columnList = "endereco_id")
})
public class Expedicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoExpedicao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusExpedicao status = StatusExpedicao.AGENDADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id")
    private Funcionario motorista;

    // Só é preenchido quando tipo = COLETA: aponta pra ENTREGA (CONCLUIDO)
    // que está sendo buscada. Uma ENTREGA só pode ter uma COLETA ativa
    // vinculada a ela (ver validação em ExpedicaoService.criar()).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_origem_id")
    private Expedicao entregaOrigem;

    // Pedido que originou essa expedição, quando gerada pelo Conferente a
    // partir de um pedido já APROVADO (fila-conferente). Um pedido pode gerar
    // MAIS DE UMA expedição quando os itens estão espalhados em depósitos
    // diferentes — nesse caso cada expedição cobre um subconjunto dos itens
    // (ver depositoOrigem abaixo e ExpedicaoService.criar()). Fica null para
    // expedições avulsas e para COLETA (que deriva da ENTREGA, não do pedido).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    // Depósito de onde os itens dessa expedição estão saindo — só é
    // preenchido quando a expedição vem de um pedido (pedido != null). É o
    // que garante que duas expedições do mesmo pedido nunca competem pelos
    // mesmos itens: cada uma cobre exatamente um depósito.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposito_origem_id")
    private Deposito depositoOrigem;

    // Pessoas autorizadas a receber o equipamento nessa entrega (até 3).
    // Definidas na criação da expedição; o Termo de Vistoria só EXIBE esses
    // nomes (não edita), e a confirmação de entrega (passo 3) usa um deles
    // como quem assina no local.
    @Column(name = "nome_autorizado_1", length = 150)
    private String nomeAutorizado1;

    @Column(name = "nome_autorizado_2", length = 150)
    private String nomeAutorizado2;

    @Column(name = "nome_autorizado_3", length = 150)
    private String nomeAutorizado3;

    @Column(name = "placa_veiculo", length = 20)
    private String placaVeiculo;

    @Column(name = "data_programada", nullable = false)
    private LocalDate dataProgramada;

    @Column(name = "horario_programado")
    private String horarioProgramado;

    // Endereço de entrega/coleta — aponta por FK pra uma linha própria da tabela
    // enderecos (cópia "avulsa"), como no Pedido. Vem copiado do pedido de
    // origem (ou digitado na hora, pra expedições avulsas / override manual).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id")
    private Endereco enderecoEntrega;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "checkout_em")
    private LocalDateTime checkoutEm;

    @Column(name = "checkin_em")
    private LocalDateTime checkinEm;

    @Column(name = "assinatura_cliente")
    private String assinaturaCliente;

    // Confirmação de entrega no local do cliente, feita pelo ENTREGADOR entre
    // o check-out (saída do depósito) e o check-in (volta do conferente).
    @Column(name = "entrega_confirmada_em")
    private LocalDateTime entregaConfirmadaEm;

    @Column(name = "assinatura_entrega")
    private String assinaturaEntrega;

    @Column(name = "foto_entrega", length = 500)
    private String fotoEntrega;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "expedicao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemExpedicao> itens = new ArrayList<>();

    @OneToMany(mappedBy = "expedicao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vistoria> vistorias = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public TipoExpedicao getTipo() { return tipo; }
    public void setTipo(TipoExpedicao tipo) { this.tipo = tipo; }

    public StatusExpedicao getStatus() { return status; }
    public void setStatus(StatusExpedicao status) { this.status = status; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Funcionario getMotorista() { return motorista; }
    public void setMotorista(Funcionario motorista) { this.motorista = motorista; }

    public Expedicao getEntregaOrigem() { return entregaOrigem; }
    public void setEntregaOrigem(Expedicao entregaOrigem) { this.entregaOrigem = entregaOrigem; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Deposito getDepositoOrigem() { return depositoOrigem; }
    public void setDepositoOrigem(Deposito depositoOrigem) { this.depositoOrigem = depositoOrigem; }

    public String getNomeAutorizado1() { return nomeAutorizado1; }
    public void setNomeAutorizado1(String nomeAutorizado1) { this.nomeAutorizado1 = nomeAutorizado1; }

    public String getNomeAutorizado2() { return nomeAutorizado2; }
    public void setNomeAutorizado2(String nomeAutorizado2) { this.nomeAutorizado2 = nomeAutorizado2; }

    public String getNomeAutorizado3() { return nomeAutorizado3; }
    public void setNomeAutorizado3(String nomeAutorizado3) { this.nomeAutorizado3 = nomeAutorizado3; }

    public String getPlacaVeiculo() { return placaVeiculo; }
    public void setPlacaVeiculo(String placaVeiculo) { this.placaVeiculo = placaVeiculo; }

    public LocalDate getDataProgramada() { return dataProgramada; }
    public void setDataProgramada(LocalDate dataProgramada) { this.dataProgramada = dataProgramada; }

    public String getHorarioProgramado() { return horarioProgramado; }
    public void setHorarioProgramado(String horarioProgramado) { this.horarioProgramado = horarioProgramado; }

    public Endereco getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(Endereco enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getCheckoutEm() { return checkoutEm; }
    public void setCheckoutEm(LocalDateTime checkoutEm) { this.checkoutEm = checkoutEm; }

    public LocalDateTime getCheckinEm() { return checkinEm; }
    public void setCheckinEm(LocalDateTime checkinEm) { this.checkinEm = checkinEm; }

    public String getAssinaturaCliente() { return assinaturaCliente; }
    public void setAssinaturaCliente(String assinaturaCliente) { this.assinaturaCliente = assinaturaCliente; }

    public LocalDateTime getEntregaConfirmadaEm() { return entregaConfirmadaEm; }
    public void setEntregaConfirmadaEm(LocalDateTime entregaConfirmadaEm) { this.entregaConfirmadaEm = entregaConfirmadaEm; }

    public String getAssinaturaEntrega() { return assinaturaEntrega; }
    public void setAssinaturaEntrega(String assinaturaEntrega) { this.assinaturaEntrega = assinaturaEntrega; }

    public String getFotoEntrega() { return fotoEntrega; }
    public void setFotoEntrega(String fotoEntrega) { this.fotoEntrega = fotoEntrega; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public List<ItemExpedicao> getItens() { return itens; }
    public void setItens(List<ItemExpedicao> itens) { this.itens = itens; }

    public List<Vistoria> getVistorias() { return vistorias; }
    public void setVistorias(List<Vistoria> vistorias) { this.vistorias = vistorias; }
}