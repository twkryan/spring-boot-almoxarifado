package br.com.almoxarifado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Uma peça do almoxarifado (cabo, periférico, ferramenta...). O saldo só muda por movimentações. */
@Entity
@Table(name = "itens")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o código.")
    @Size(max = 30, message = "Use até 30 caracteres.")
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "Use até 120 caracteres.")
    @Column(nullable = false, length = 120)
    private String nome;

    @Size(max = 500, message = "Use até 500 caracteres.")
    @Column(length = 500)
    private String descricao;

    @NotNull(message = "Escolha a categoria.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @NotNull(message = "Escolha o tipo de controle.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoItem tipo;

    @NotBlank(message = "Informe a unidade.")
    @Size(max = 10, message = "Use até 10 caracteres.")
    @Column(nullable = false, length = 10)
    private String unidade = "un";

    @Min(value = 0, message = "Não pode ser negativo.")
    private int quantidade;

    @Min(value = 0, message = "Não pode ser negativo.")
    private int estoqueMinimo;

    @NotNull(message = "Informe o custo unitário.")
    @DecimalMin(value = "0.00", message = "Não pode ser negativo.")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Size(max = 60, message = "Use até 60 caracteres.")
    @Column(length = 60)
    private String localizacao;

    private boolean ativo = true;

    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public boolean isAbaixoMinimo() {
        return quantidade <= estoqueMinimo;
    }

    public boolean isZerado() {
        return quantidade <= 0;
    }

    public BigDecimal getValorEmEstoque() {
        return custoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Nível do estoque em % considerando 3x o mínimo como "cheio". Usado nas barras de progresso. */
    public int getNivelEstoque() {
        int referencia = Math.max(1, estoqueMinimo * 3);
        return Math.max(0, Math.min(100, quantidade * 100 / referencia));
    }

    public String getStatusLabel() {
        if (isZerado()) return "Zerado";
        if (isAbaixoMinimo()) return "Repor";
        return "Normal";
    }

    public String getStatusCss() {
        if (isZerado()) return "danger";
        if (isAbaixoMinimo()) return "warn";
        return "ok";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public TipoItem getTipo() { return tipo; }
    public void setTipo(TipoItem tipo) { this.tipo = tipo; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(int estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
