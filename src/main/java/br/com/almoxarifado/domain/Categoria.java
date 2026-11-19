package br.com.almoxarifado.domain;

public enum Categoria {
    CABO("Cabos", "cable"),
    PERIFERICO("Periféricos", "mouse"),
    FERRAMENTA("Ferramentas", "wrench"),
    COMPONENTE("Componentes", "chip"),
    REDE("Rede", "network"),
    INSUMO("Insumos", "box");

    private final String label;
    private final String icone;

    Categoria(String label, String icone) {
        this.label = label;
        this.icone = icone;
    }

    public String getLabel() {
        return label;
    }

    public String getIcone() {
        return icone;
    }
}
