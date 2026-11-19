package br.com.almoxarifado.web;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Converte valores em reais digitados do jeito brasileiro ("32,90", "1.234,56", "R$ 32,90")
 * e também aceita ponto decimal ("32.90"). Exibe de volta no formulário como "32,90".
 */
public class MoedaEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String texto) {
        if (texto == null || texto.isBlank()) {
            setValue(null);
            return;
        }
        String s = texto.replace("R$", "").replace(" ", "").replace(" ", "").trim();
        if (s.contains(",")) {
            s = s.replace(".", "").replace(",", ".");          // 1.234,56 -> 1234.56
        } else if (s.matches("\\d{1,3}(\\.\\d{3})+")) {
            s = s.replace(".", "");                            // 1.234 -> 1234
        }
        try {
            setValue(new BigDecimal(s).setScale(2, RoundingMode.HALF_UP));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: " + texto, e);
        }
    }

    @Override
    public String getAsText() {
        Object v = getValue();
        if (!(v instanceof BigDecimal valor)) return "";
        DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        return df.format(valor);
    }
}
