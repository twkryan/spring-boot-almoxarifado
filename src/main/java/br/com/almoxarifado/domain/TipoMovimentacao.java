package br.com.almoxarifado.domain;

public enum TipoMovimentacao {
    SALDO_INICIAL("Saldo inicial", "neutral", 1),
    ENTRADA("Reposição", "ok", 1),
    CONSUMO("Consumo", "warn", -1),
    EMPRESTIMO("Empréstimo", "info", -1),
    DEVOLUCAO("Devolução", "accent", 1),
    /** A quantidade do ajuste já vem com sinal (positiva ou negativa). */
    AJUSTE("Ajuste de inventário", "danger", 1);

    private final String label;
    private final String css;
    private final int sinal;

    TipoMovimentacao(String label, String css, int sinal) {
        this.label = label;
        this.css = css;
        this.sinal = sinal;
    }

    public String getLabel() {
        return label;
    }

    public String getCss() {
        return css;
    }

    public int getSinal() {
        return sinal;
    }

    public boolean isRetirada() {
        return this == CONSUMO || this == EMPRESTIMO;
    }
}
