package br.com.almoxarifado;

import br.com.almoxarifado.web.MoedaEditor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoedaEditorTest {

    private BigDecimal ler(String texto) {
        MoedaEditor e = new MoedaEditor();
        e.setAsText(texto);
        return (BigDecimal) e.getValue();
    }

    @Test
    void aceitaFormatoBrasileiro() {
        assertThat(ler("32,90")).isEqualByComparingTo("32.90");
        assertThat(ler("1.234,56")).isEqualByComparingTo("1234.56");
        assertThat(ler("R$ 1.234,56")).isEqualByComparingTo("1234.56");
        assertThat(ler("1.234")).isEqualByComparingTo("1234");
    }

    @Test
    void aceitaPontoDecimal() {
        assertThat(ler("32.90")).isEqualByComparingTo("32.90");
        assertThat(ler("89.9")).isEqualByComparingTo("89.90");
    }

    @Test
    void vazioViraNuloETextoInvalidoFalha() {
        assertThat(ler("  ")).isNull();
        assertThatThrownBy(() -> ler("abc")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exibeComVirgula() {
        MoedaEditor e = new MoedaEditor();
        e.setValue(new BigDecimal("1234.5"));
        assertThat(e.getAsText()).isEqualTo("1234,50");
    }
}
