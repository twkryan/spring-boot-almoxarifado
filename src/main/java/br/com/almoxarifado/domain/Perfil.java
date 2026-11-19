package br.com.almoxarifado.domain;

public enum Perfil {
    ADMIN("Administrador"),
    OPERADOR("Operador");

    private final String label;

    Perfil(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
