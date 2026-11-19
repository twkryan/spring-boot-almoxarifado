package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.Item;
import br.com.almoxarifado.repository.EmprestimoRepository;
import br.com.almoxarifado.repository.ItemRepository;
import br.com.almoxarifado.repository.MovimentacaoRepository;
import br.com.almoxarifado.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;

@Controller
public class DashboardController {

    private final RelatorioService relatorios;
    private final ItemRepository itens;
    private final EmprestimoRepository emprestimos;
    private final MovimentacaoRepository movimentacoes;

    private final br.com.almoxarifado.repository.SetorRepository setores;
    private final br.com.almoxarifado.repository.TecnicoRepository tecnicos;

    public DashboardController(RelatorioService relatorios, ItemRepository itens,
                               EmprestimoRepository emprestimos, MovimentacaoRepository movimentacoes,
                               br.com.almoxarifado.repository.SetorRepository setores,
                               br.com.almoxarifado.repository.TecnicoRepository tecnicos) {
        this.setores = setores;
        this.tecnicos = tecnicos;
        this.relatorios = relatorios;
        this.itens = itens;
        this.emprestimos = emprestimos;
        this.movimentacoes = movimentacoes;
    }

    @GetMapping("/")
    public String painel(Model model) {
        model.addAttribute("resumo", relatorios.resumo());
        model.addAttribute("qtdSetores", setores.count());
        model.addAttribute("qtdTecnicos", tecnicos.count());
        model.addAttribute("custos", relatorios.custosMensais(6));
        model.addAttribute("recentes", movimentacoes.findTop8ByOrderByDataHoraDescIdDesc());
        model.addAttribute("alertas", itens.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(Item::isAbaixoMinimo)
                .sorted(Comparator.comparingInt(Item::getNivelEstoque))
                .limit(6)
                .toList());
        model.addAttribute("vencendo", emprestimos.findByDataDevolucaoIsNullOrderByDataPrevistaAsc().stream()
                .limit(6)
                .toList());
        return "dashboard";
    }
}
