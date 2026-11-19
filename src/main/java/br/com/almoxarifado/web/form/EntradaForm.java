package br.com.almoxarifado.web.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class EntradaForm {

    @NotNull(message = "Selecione o item.")
    private Long itemId;

    @Min(value = 1, message = "Mínimo de 1.")
    private int quantidade = 1;

    @DecimalMin(value = "0.00", message = "Não pode ser negativo.")
    private BigDecimal custoUnitario;

    @Size(max = 300, message = "Use até 300 caracteres.")
    private String observacao;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
