package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.*;
import br.com.almoxarifado.repository.EmprestimoRepository;
import br.com.almoxarifado.repository.ItemRepository;
import br.com.almoxarifado.repository.MovimentacaoRepository;
import br.com.almoxarifado.service.EstoqueException;
import br.com.almoxarifado.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/itens")
public class ItemController {

    private final ItemRepository itens;
    private final MovimentacaoRepository movimentacoes;
    private final EmprestimoRepository emprestimos;
    private final EstoqueService estoque;
    private final UsuarioAtual usuarioAtual;

    public ItemController(ItemRepository itens, MovimentacaoRepository movimentacoes,
                          EmprestimoRepository emprestimos, EstoqueService estoque, UsuarioAtual usuarioAtual) {
        this.itens = itens;
        this.movimentacoes = movimentacoes;
        this.emprestimos = emprestimos;
        this.estoque = estoque;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) Categoria categoria,
                        @RequestParam(required = false) TipoItem tipo,
                        @RequestParam(defaultValue = "false") boolean baixo,
                        Model model) {
        List<Item> resultado = itens.buscar(q.trim().toLowerCase(), categoria, tipo);
        if (baixo) {
            resultado = resultado.stream().filter(Item::isAbaixoMinimo).toList();
        }
        List<Item> todos = itens.findByAtivoTrueOrderByNomeAsc();
        model.addAttribute("itens", resultado);
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        model.addAttribute("tipo", tipo);
        model.addAttribute("baixo", baixo);
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("tipos", TipoItem.values());
        model.addAttribute("totalItens", todos.size());
        model.addAttribute("totalAbaixo", todos.stream().filter(Item::isAbaixoMinimo).count());
        model.addAttribute("totalZerados", todos.stream().filter(Item::isZerado).count());
        model.addAttribute("valorTotal", todos.stream().map(Item::getValorEmEstoque).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("valorFiltrado", resultado.stream().map(Item::getValorEmEstoque).reduce(BigDecimal.ZERO, BigDecimal::add));
        return "itens/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Item item = new Item();
        item.setEstoqueMinimo(5);
        item.setTipo(TipoItem.CONSUMIVEL);
        return form(item, model);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return form(buscar(id), model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("item") Item item, BindingResult result, Model model,
                        Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) return form(item, model);
        try {
            Item salvo = estoque.cadastrarItem(item, usuarioAtual.nome(principal));
            redirect.addFlashAttribute("sucesso", "Item " + salvo.getNome() + " cadastrado.");
            return "redirect:/itens/" + salvo.getId();
        } catch (EstoqueException e) {
            result.rejectValue("codigo", "codigo.duplicado", e.getMessage());
            return form(item, model);
        }
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("item") Item item, BindingResult result,
                            Model model, RedirectAttributes redirect) {
        item.setId(id);
        if (result.hasErrors()) return form(item, model);
        try {
            estoque.atualizarItem(id, item);
            redirect.addFlashAttribute("sucesso", "Alterações salvas.");
            return "redirect:/itens/" + id;
        } catch (EstoqueException e) {
            result.rejectValue("codigo", "codigo.duplicado", e.getMessage());
            return form(item, model);
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Item item = buscar(id);
        List<Movimentacao> historico = movimentacoes.findByItemOrderByDataHoraDescIdDesc(item);
        model.addAttribute("item", item);
        model.addAttribute("historico", historico.stream().limit(40).toList());
        model.addAttribute("totalMovimentos", historico.size());
        model.addAttribute("emprestados", emprestimos.findByItemAndDataDevolucaoIsNullOrderByDataPrevistaAsc(item));
        model.addAttribute("unidadesRetiradas", historico.stream()
                .filter(m -> m.getTipo().isRetirada()).mapToLong(Movimentacao::getQuantidade).sum());
        model.addAttribute("custoReposto", historico.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.ENTRADA)
                .map(Movimentacao::getCustoTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        return "itens/detalhe";
    }

    @PostMapping("/{id}/ajuste")
    public String ajustar(@PathVariable Long id, @RequestParam(required = false) Integer contado,
                          @RequestParam(defaultValue = "") String justificativa,
                          Principal principal, RedirectAttributes redirect) {
        try {
            if (contado == null) throw new EstoqueException("Informe a quantidade contada.");
            Movimentacao m = estoque.ajustarInventario(id, contado, justificativa, usuarioAtual.nome(principal));
            redirect.addFlashAttribute("sucesso", "Inventário ajustado (" + (m.getQuantidade() > 0 ? "+" : "")
                    + m.getQuantidade() + "). Novo saldo: " + m.getSaldoApos() + ".");
        } catch (EstoqueException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/itens/" + id + "#inventario";
    }

    @PostMapping("/{id}/arquivar")
    public String arquivar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            estoque.desativarItem(id);
            redirect.addFlashAttribute("sucesso", "Item arquivado. O histórico continua nos relatórios.");
            return "redirect:/itens";
        } catch (EstoqueException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/itens/" + id;
        }
    }

    private String form(Item item, Model model) {
        model.addAttribute("item", item);
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("tipos", TipoItem.values());
        return "itens/form";
    }

    private Item buscar(Long id) {
        return itens.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado"));
    }
}
