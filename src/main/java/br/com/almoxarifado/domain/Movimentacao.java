package br.com.almoxarifado.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro imutável de tudo que alterou o saldo de um item. Base da auditoria e dos relatórios. */
@Entity
@Table(name = "movimentacoes", indexes = {
        @Index(name = "idx_mov_data", columnList = "dataHora"),
        @Index(name = "idx_mov_tipo_data", columnList = "tipo,dataHora")
})
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @ManyToOne(optional = false)
    private Item item;

    @ManyToOne
    private Tecnico tecnico;

    @ManyToOne
    private Setor setor;

    @ManyToOne
    private Emprestimo emprestimo;

    /** Quantidade movimentada. Positiva, exceto em ajustes, onde carrega o sinal da correção. */
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal custoTotal = BigDecimal.ZERO;

    private int saldoApos;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 80)
    private String usuario;

    @Column(length = 300)
    private String observacao;

    /** Variação no saldo causada por esta movimentação. */
    public int getVariacao() {
        return tipo.getSinal() * quantidade;
    }

    public String getResponsavel() {
        if (tecnico != null) return tecnico.getNome();
        return setor != null ? setor.getNome() : "—";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Tecnico getTecnico() { return tecnico; }
    public void setTecnico(Tecnico tecnico) { this.tecnico = tecnico; }
    public Setor getSetor() { return setor; }
    public void setSetor(Setor setor) { this.setor = setor; }
    public Emprestimo getEmprestimo() { return emprestimo; }
    public void setEmprestimo(Emprestimo emprestimo) { this.emprestimo = emprestimo; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public BigDecimal getCustoTotal() { return custoTotal; }
    public void setCustoTotal(BigDecimal custoTotal) { this.custoTotal = custoTotal; }
    public int getSaldoApos() { return saldoApos; }
    public void setSaldoApos(int saldoApos) { this.saldoApos = saldoApos; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
