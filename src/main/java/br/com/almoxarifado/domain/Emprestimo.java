package br.com.almoxarifado.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "emprestimos")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Item item;

    @ManyToOne
    private Tecnico tecnico;

    @ManyToOne
    private Setor setor;

    private int quantidade;

    @Column(nullable = false)
    private LocalDateTime dataRetirada;

    @Column(nullable = false)
    private LocalDate dataPrevista;

    private LocalDateTime dataDevolucao;

    @Column(length = 300)
    private String observacao;

    @Column(length = 80)
    private String registradoPor;

    @Column(length = 80)
    private String recebidoPor;

    /** Condição do equipamento informada na devolução (avarias, falta de peças...). */
    @Column(length = 300)
    private String observacaoDevolucao;

    public StatusEmprestimo getStatus() {
        if (dataDevolucao != null) return StatusEmprestimo.DEVOLVIDO;
        if (LocalDate.now().isAfter(dataPrevista)) return StatusEmprestimo.ATRASADO;
        return StatusEmprestimo.ABERTO;
    }

    public boolean isAberto() {
        return dataDevolucao == null;
    }

    /** Dias até o prazo (negativo quando atrasado). */
    public long getDiasParaDevolucao() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dataPrevista);
    }

    /** Percentual do prazo já consumido, para a barra de progresso. */
    public int getProgressoPrazo() {
        long total = Math.max(1, ChronoUnit.DAYS.between(dataRetirada.toLocalDate(), dataPrevista));
        LocalDate fim = dataDevolucao != null ? dataDevolucao.toLocalDate() : LocalDate.now();
        long passado = ChronoUnit.DAYS.between(dataRetirada.toLocalDate(), fim);
        return (int) Math.max(4, Math.min(100, passado * 100 / total));
    }

    public String getResponsavel() {
        if (tecnico != null) return tecnico.getNome();
        return setor != null ? setor.getNome() : "—";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Tecnico getTecnico() { return tecnico; }
    public void setTecnico(Tecnico tecnico) { this.tecnico = tecnico; }
    public Setor getSetor() { return setor; }
    public void setSetor(Setor setor) { this.setor = setor; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public LocalDateTime getDataRetirada() { return dataRetirada; }
    public void setDataRetirada(LocalDateTime dataRetirada) { this.dataRetirada = dataRetirada; }
    public LocalDate getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(LocalDate dataPrevista) { this.dataPrevista = dataPrevista; }
    public LocalDateTime getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(LocalDateTime dataDevolucao) { this.dataDevolucao = dataDevolucao; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public String getRecebidoPor() { return recebidoPor; }
    public void setRecebidoPor(String recebidoPor) { this.recebidoPor = recebidoPor; }
    public String getObservacaoDevolucao() { return observacaoDevolucao; }
    public void setObservacaoDevolucao(String observacaoDevolucao) { this.observacaoDevolucao = observacaoDevolucao; }
}
