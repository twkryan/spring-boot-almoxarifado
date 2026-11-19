package br.com.almoxarifado.service;

/** Regra de negócio violada (saldo insuficiente, código duplicado...). A mensagem é exibida ao usuário. */
public class EstoqueException extends RuntimeException {

    public EstoqueException(String mensagem) {
        super(mensagem);
    }
}
