package br.com.almoxarifado.web.form;

import br.com.almoxarifado.domain.TipoMovimentacao;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class RetiradaForm {

    @NotNull(message = "Escolha o tipo de retirada.")
    private TipoMovimentacao tipo = TipoMovimentacao.CONSUMO;

    @NotNull(message = "Selecione o item.")
    private Long itemId;

    private Long tecnicoId;

    private Long setorId;

    @Min(value = 1, message = "Mínimo de 1.")
    private int quantidade = 1;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPrevista = LocalDate.now().plusDays(7);

    @Size(max = 300, message = "Use até 300 caracteres.")
    private String observacao;

    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }
    public Long getSetorId() { return setorId; }
    public void setSetorId(Long setorId) { this.setorId = setorId; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public LocalDate getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(LocalDate dataPrevista) { this.dataPrevista = dataPrevista; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
