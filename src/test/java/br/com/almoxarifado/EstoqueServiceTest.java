package br.com.almoxarifado;

import br.com.almoxarifado.domain.*;
import br.com.almoxarifado.repository.ItemRepository;
import br.com.almoxarifado.repository.SetorRepository;
import br.com.almoxarifado.repository.TecnicoRepository;
import br.com.almoxarifado.service.EstoqueException;
import br.com.almoxarifado.service.EstoqueService;
import br.com.almoxarifado.service.RelatorioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste;DB_CLOSE_DELAY=-1"
})
@Transactional
class EstoqueServiceTest {

    @Autowired EstoqueService estoque;
    @Autowired RelatorioService relatorios;
    @Autowired ItemRepository itens;
    @Autowired SetorRepository setores;
    @Autowired TecnicoRepository tecnicos;

    Tecnico tecnico;

    @BeforeEach
    void preparar() {
        Setor setor = new Setor();
        setor.setNome("Suporte");
        setores.save(setor);
        tecnico = new Tecnico();
        tecnico.setNome("Ana Teste");
        tecnico.setMatricula("T-1");
        tecnico.setSetor(setor);
        tecnicos.save(tecnico);
    }

    private Item novoItem(String codigo, TipoItem tipo, int saldo) {
        Item i = new Item();
        i.setCodigo(codigo);
        i.setNome("Item " + codigo);
        i.setCategoria(Categoria.CABO);
        i.setTipo(tipo);
        i.setQuantidade(saldo);
        i.setEstoqueMinimo(2);
        i.setCustoUnitario(new BigDecimal("10.00"));
        return estoque.cadastrarItem(i, "teste");
    }

    @Test
    void consumoBaixaSaldoECalculaCustoEUsaSetorDoTecnico() {
        Item item = novoItem("C1", TipoItem.CONSUMIVEL, 10);
        Movimentacao m = estoque.registrarConsumo(item.getId(), tecnico.getId(), null, 3, null, "teste");

        assertThat(itens.findById(item.getId()).orElseThrow().getQuantidade()).isEqualTo(7);
        assertThat(m.getCustoTotal()).isEqualByComparingTo("30.00");
        assertThat(m.getSetor().getNome()).isEqualTo("Suporte");
    }

    @Test
    void naoPermiteRetirarMaisQueOSaldo() {
        Item item = novoItem("C2", TipoItem.CONSUMIVEL, 2);
        assertThatThrownBy(() -> estoque.registrarConsumo(item.getId(), tecnico.getId(), null, 5, null, "teste"))
                .isInstanceOf(EstoqueException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void emprestimoEDevolucaoRestauramOSaldo() {
        Item item = novoItem("R1", TipoItem.RETORNAVEL, 3);
        Emprestimo e = estoque.registrarEmprestimo(item.getId(), tecnico.getId(), null, 1,
                LocalDate.now().plusDays(5), null, "teste");
        assertThat(itens.findById(item.getId()).orElseThrow().getQuantidade()).isEqualTo(2);

        estoque.registrarDevolucao(e.getId(), "teste");
        assertThat(itens.findById(item.getId()).orElseThrow().getQuantidade()).isEqualTo(3);
        assertThatThrownBy(() -> estoque.registrarDevolucao(e.getId(), "teste"))
                .isInstanceOf(EstoqueException.class);
    }

    @Test
    void tipoDoItemDefineAFormaDeSaida() {
        Item consumivel = novoItem("C3", TipoItem.CONSUMIVEL, 5);
        Item retornavel = novoItem("R2", TipoItem.RETORNAVEL, 5);
        assertThatThrownBy(() -> estoque.registrarEmprestimo(consumivel.getId(), tecnico.getId(), null, 1,
                LocalDate.now().plusDays(1), null, "teste")).isInstanceOf(EstoqueException.class);
        assertThatThrownBy(() -> estoque.registrarConsumo(retornavel.getId(), tecnico.getId(), null, 1, null, "teste"))
                .isInstanceOf(EstoqueException.class);
    }

    @Test
    void auditoriaRecalculaSaldoAPartirDasMovimentacoes() {
        Item item = novoItem("C4", TipoItem.CONSUMIVEL, 10);
        estoque.registrarConsumo(item.getId(), tecnico.getId(), null, 4, null, "teste");
        estoque.registrarEntrada(item.getId(), 6, new BigDecimal("12.00"), "NF 1", "teste");
        estoque.ajustarInventario(item.getId(), 11, "1 unidade avariada", "teste");

        var linha = relatorios.auditoria().stream()
                .filter(a -> a.item().getId().equals(item.getId())).findFirst().orElseThrow();
        assertThat(linha.item().getQuantidade()).isEqualTo(11);
        assertThat(linha.saldoCalculado()).isEqualTo(11);
        assertThat(linha.divergente()).isFalse();
    }

    @Test
    void reposicaoEntraNoCustoDoMes() {
        Item item = novoItem("C5", TipoItem.CONSUMIVEL, 0);
        estoque.registrarEntrada(item.getId(), 5, new BigDecimal("20.00"), null, "teste");
        var mesAtual = relatorios.custosMensais(1).get(0);
        assertThat(mesAtual.reposicao()).isEqualByComparingTo("100.00");
    }
}
