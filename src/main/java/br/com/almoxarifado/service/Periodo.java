package br.com.almoxarifado.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Intervalo de datas fechado nas duas pontas (de 00:00 do primeiro dia ao fim do último). */
public record Periodo(LocalDate de, LocalDate ate) {

    public static Periodo ou(LocalDate de, LocalDate ate, int diasPadrao) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        LocalDate inicio = de != null ? de : fim.minusDays(diasPadrao - 1L);
        if (inicio.isAfter(fim)) {
            LocalDate tmp = inicio;
            inicio = fim;
            fim = tmp;
        }
        return new Periodo(inicio, fim);
    }

    public LocalDateTime inicio() {
        return de.atStartOfDay();
    }

    public LocalDateTime fim() {
        return ate.plusDays(1).atStartOfDay();
    }
}
