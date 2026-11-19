package br.com.almoxarifado.domain;

public enum StatusEmprestimo {
    ABERTO("Em uso", "info"),
    ATRASADO("Atrasado", "danger"),
    DEVOLVIDO("Devolvido", "ok");

    private final String label;
    private final String css;

    StatusEmprestimo(String label, String css) {
        this.label = label;
        this.css = css;
    }

    public String getLabel() {
        return label;
    }

    public String getCss() {
        return css;
    }
}
