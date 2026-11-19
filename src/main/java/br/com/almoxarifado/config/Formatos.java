package br.com.almoxarifado.config;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação pt-BR usada nos templates como {@code ${@fmt.moeda(valor)}}. */
@Component("fmt")
public class Formatos {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd MMM", PT_BR);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", PT_BR);
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM/yyyy", PT_BR);
    private static final DateTimeFormatter MES_CURTO = DateTimeFormatter.ofPattern("MMM", PT_BR);
    private static final DateTimeFormatter MES_LONGO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR);

    public String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor == null ? BigDecimal.ZERO : valor);
    }

    public String numero(long valor) {
        return NumberFormat.getIntegerInstance(PT_BR).format(valor);
    }

    public String decimal(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor == null ? BigDecimal.ZERO : valor);
    }

    public String data(LocalDate d) {
        return d == null ? "—" : DATA.format(d);
    }

    public String data(LocalDateTime d) {
        return d == null ? "—" : DATA.format(d);
    }

    public String dataCurta(LocalDate d) {
        return d == null ? "—" : limpar(DATA_CURTA.format(d));
    }

    public String dataCurta(LocalDateTime d) {
        return d == null ? "—" : dataCurta(d.toLocalDate());
    }

    public String dataHora(LocalDateTime d) {
        return d == null ? "—" : DATA_HORA.format(d);
    }

    public String hora(LocalDateTime d) {
        return d == null ? "" : HORA.format(d);
    }

    public String mes(YearMonth m) {
        return capitalizar(limpar(MES.format(m)));
    }

    public String mesCurto(YearMonth m) {
        return capitalizar(limpar(MES_CURTO.format(m)));
    }

    public String mesLongo(YearMonth m) {
        return capitalizar(MES_LONGO.format(m));
    }

    public String relativo(LocalDateTime d) {
        if (d == null) return "—";
        Duration dur = Duration.between(d, LocalDateTime.now());
        long min = dur.toMinutes();
        if (min < 1) return "agora";
        if (min < 60) return "há " + min + " min";
        long horas = dur.toHours();
        if (horas < 24) return "há " + horas + " h";
        long dias = dur.toDays();
        if (dias == 1) return "ontem";
        if (dias < 30) return "há " + dias + " dias";
        return data(d);
    }

    /** "1 item" / "3 itens": concordância em vez de "item(ns)". */
    public String plural(long n, String singular, String plural) {
        return numero(n) + " " + (n == 1 ? singular : plural);
    }

    /** Texto do prazo de devolução a partir dos dias restantes (negativo = atraso). */
    public String prazo(long dias) {
        if (dias < 0) return plural(-dias, "dia", "dias") + " de atraso";
        if (dias == 0) return "vence hoje";
        if (dias == 1) return "vence amanhã";
        return "em " + dias + " dias";
    }

    public String variacao(Integer pct) {
        if (pct == null) return "";
        return (pct > 0 ? "▲ " : pct < 0 ? "▼ " : "") + Math.abs(pct) + "%";
    }

    public String sinal(long valor) {
        return valor > 0 ? "+" + valor : String.valueOf(valor);
    }

    public String iniciais(String nome) {
        if (nome == null || nome.isBlank()) return "?";
        String[] partes = nome.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + ultima).toUpperCase(PT_BR);
    }

    /** Classe CSS de cor estável para o avatar de um nome. */
    public String cor(String nome) {
        return "av-" + Math.floorMod(nome == null ? 0 : nome.hashCode(), 6);
    }

    private static String limpar(String s) {
        return s.replace(".", "");
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(PT_BR) + s.substring(1);
    }
}
