package br.com.almoxarifado.domain;

/** Define como a peça sai do almoxarifado. */
public enum TipoItem {
    RETORNAVEL("Retornável", "Sai por empréstimo e volta ao estoque"),
    CONSUMIVEL("Consumível", "Sai por consumo, com baixa definitiva");

    private final String label;
    private final String descricao;

    TipoItem(String label, String descricao) {
        this.label = label;
        this.descricao = descricao;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }
}
