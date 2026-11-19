package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.Item;
import br.com.almoxarifado.domain.Movimentacao;
import br.com.almoxarifado.domain.TipoItem;
import br.com.almoxarifado.domain.TipoMovimentacao;
import br.com.almoxarifado.repository.ItemRepository;
import br.com.almoxarifado.repository.SetorRepository;
import br.com.almoxarifado.repository.TecnicoRepository;
import br.com.almoxarifado.service.EstoqueException;
import br.com.almoxarifado.service.EstoqueService;
import br.com.almoxarifado.service.Periodo;
import br.com.almoxarifado.service.RelatorioService;
import br.com.almoxarifado.web.form.EntradaForm;
import br.com.almoxarifado.web.form.RetiradaForm;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Controller
public class MovimentacaoController {

    private final EstoqueService estoque;
    private final RelatorioService relatorios;
    private final ItemRepository itens;
    private final TecnicoRepository tecnicos;
    private final SetorRepository setores;
    private final UsuarioAtual usuarioAtual;

    public MovimentacaoController(EstoqueService estoque, RelatorioService relatorios, ItemRepository itens,
                                  TecnicoRepository tecnicos, SetorRepository setores, UsuarioAtual usuarioAtual) {
        this.estoque = estoque;
        this.relatorios = relatorios;
        this.itens = itens;
        this.tecnicos = tecnicos;
        this.setores = setores;
        this.usuarioAtual = usuarioAtual;
    }

    // ---------------------------------------------------------------- retirada (consumo / empréstimo)

    @GetMapping("/retiradas/nova")
    public String novaRetirada(@RequestParam(required = false) Long itemId,
                               @RequestParam(required = false) TipoMovimentacao tipo, Model model) {
        RetiradaForm form = new RetiradaForm();
        form.setItemId(itemId);
        if (tipo != null && tipo.isRetirada()) {
            form.setTipo(tipo);
        }
        if (itemId != null) {
            itens.findById(itemId).ifPresent(item -> form.setTipo(
                    item.getTipo() == TipoItem.RETORNAVEL ? TipoMovimentacao.EMPRESTIMO : TipoMovimentacao.CONSUMO));
        }
        return formRetirada(form, model);
    }

    @PostMapping("/retiradas")
    public String registrarRetirada(@Valid @ModelAttribute("form") RetiradaForm form, BindingResult result,
                                    Model model, Principal principal, RedirectAttributes redirect) {
        if (form.getTipo() != null && !form.getTipo().isRetirada()) {
            result.rejectValue("tipo", "tipo.invalido", "Escolha consumo ou empréstimo.");
        }
        if (result.hasErrors()) return formRetirada(form, model);
        try {
            String usuario = usuarioAtual.nome(principal);
            if (form.getTipo() == TipoMovimentacao.EMPRESTIMO) {
                var e = estoque.registrarEmprestimo(form.getItemId(), form.getTecnicoId(), form.getSetorId(),
                        form.getQuantidade(), form.getDataPrevista(), form.getObservacao(), usuario);
                redirect.addFlashAttribute("sucesso", "Empréstimo de " + e.getItem().getNome() + " para "
                        + e.getResponsavel() + " registrado. Devolução até " + e.getDataPrevista().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + ".");
                return "redirect:/emprestimos";
            }
            Movimentacao m = estoque.registrarConsumo(form.getItemId(), form.getTecnicoId(), form.getSetorId(),
                    form.getQuantidade(), form.getObservacao(), usuario);
            redirect.addFlashAttribute("sucesso", "Consumo registrado: " + m.getQuantidade() + " "
                    + m.getItem().getUnidade() + " de " + m.getItem().getNome() + ". Saldo: " + m.getSaldoApos() + ".");
            return "redirect:/historico";
        } catch (EstoqueException e) {
            result.reject("negocio", e.getMessage());
            return formRetirada(form, model);
        }
    }

    private String formRetirada(RetiradaForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("itens", itens.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("tecnicos", tecnicos.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("setores", setores.findAllByOrderByNomeAsc());
        model.addAttribute("hoje", LocalDate.now());
        return "movimentacoes/retirada";
    }

    // ---------------------------------------------------------------- reposição (entrada)

    @GetMapping("/reposicoes/nova")
    public String novaReposicao(@RequestParam(required = false) Long itemId, Model model) {
        EntradaForm form = new EntradaForm();
        form.setItemId(itemId);
        if (itemId != null) {
            itens.findById(itemId).ifPresent(item -> {
                form.setCustoUnitario(item.getCustoUnitario());
                form.setQuantidade(Math.max(1, item.getEstoqueMinimo() * 3 - item.getQuantidade()));
            });
        }
        return formEntrada(form, model);
    }

    @PostMapping("/reposicoes")
    public String registrarReposicao(@Valid @ModelAttribute("form") EntradaForm form, BindingResult result,
                                     Model model, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) return formEntrada(form, model);
        try {
            Movimentacao m = estoque.registrarEntrada(form.getItemId(), form.getQuantidade(), form.getCustoUnitario(),
                    form.getObservacao(), usuarioAtual.nome(principal));
            redirect.addFlashAttribute("sucesso", "Reposição registrada: +" + m.getQuantidade() + " "
                    + m.getItem().getUnidade() + " de " + m.getItem().getNome() + ". Saldo: " + m.getSaldoApos() + ".");
            return "redirect:/itens/" + m.getItem().getId();
        } catch (EstoqueException e) {
            result.reject("negocio", e.getMessage());
            return formEntrada(form, model);
        }
    }

    private String formEntrada(EntradaForm form, Model model) {
        List<Item> lista = itens.findByAtivoTrueOrderByNomeAsc();
        model.addAttribute("form", form);
        model.addAttribute("itens", lista);
        model.addAttribute("sugestoes", lista.stream().filter(Item::isAbaixoMinimo).limit(8).toList());
        return "movimentacoes/entrada";
    }

    // ---------------------------------------------------------------- histórico

    @GetMapping("/historico")
    public String historico(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                            @RequestParam(required = false) TipoMovimentacao tipo,
                            @RequestParam(required = false) Long tecnicoId,
                            @RequestParam(required = false) Long setorId,
                            @RequestParam(defaultValue = "") String q,
                            Model model) {
        Periodo periodo = Periodo.ou(de, ate, 30);
        Set<TipoMovimentacao> tipos = tipo == null ? null : EnumSet.of(tipo);
        List<Movimentacao> movs = relatorios.movimentacoes(periodo, tipos, tecnicoId, setorId, q);
        model.addAttribute("movs", movs);
        model.addAttribute("resumo", relatorios.resumir(movs));
        model.addAttribute("periodo", periodo);
        model.addAttribute("tipo", tipo);
        model.addAttribute("tecnicoId", tecnicoId);
        model.addAttribute("setorId", setorId);
        model.addAttribute("q", q);
        model.addAttribute("tiposMov", TipoMovimentacao.values());
        model.addAttribute("tecnicos", tecnicos.findAllByOrderByAtivoDescNomeAsc());
        model.addAttribute("setores", setores.findAllByOrderByNomeAsc());
        return "movimentacoes/historico";
    }
}
