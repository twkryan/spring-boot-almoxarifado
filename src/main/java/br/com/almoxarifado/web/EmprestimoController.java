package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.Emprestimo;
import br.com.almoxarifado.domain.StatusEmprestimo;
import br.com.almoxarifado.repository.EmprestimoRepository;
import br.com.almoxarifado.service.EstoqueException;
import br.com.almoxarifado.service.EstoqueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/emprestimos")
public class EmprestimoController {

    private final EmprestimoRepository emprestimos;
    private final EstoqueService estoque;
    private final UsuarioAtual usuarioAtual;

    public EmprestimoController(EmprestimoRepository emprestimos, EstoqueService estoque, UsuarioAtual usuarioAtual) {
        this.emprestimos = emprestimos;
        this.estoque = estoque;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "abertos") String status, Model model) {
        List<Emprestimo> abertos = emprestimos.findByDataDevolucaoIsNullOrderByDataPrevistaAsc();
        List<Emprestimo> lista = switch (status) {
            case "atrasados" -> abertos.stream().filter(e -> e.getStatus() == StatusEmprestimo.ATRASADO).toList();
            case "devolvidos" -> emprestimos.findByDataDevolucaoIsNotNullOrderByDataDevolucaoDesc().stream().limit(100).toList();
            case "todos" -> emprestimos.findAllByOrderByDataRetiradaDesc().stream().limit(150).toList();
            default -> abertos;
        };
        LocalDate hoje = LocalDate.now();
        model.addAttribute("lista", lista);
        model.addAttribute("status", status);
        model.addAttribute("qtdAbertos", abertos.size());
        model.addAttribute("qtdAtrasados", abertos.stream().filter(e -> e.getStatus() == StatusEmprestimo.ATRASADO).count());
        model.addAttribute("qtdVenceSemana", abertos.stream()
                .filter(e -> !e.getDataPrevista().isBefore(hoje) && !e.getDataPrevista().isAfter(hoje.plusDays(7))).count());
        model.addAttribute("qtdDevolvidos", emprestimos.countByDataDevolucaoIsNotNull());
        return "emprestimos/lista";
    }

    /** Tela de confirmação: a devolução é irreversível, então pede um segundo passo e a condição do equipamento. */
    @GetMapping("/{id}/devolver")
    public String confirmarDevolucao(@PathVariable Long id, @RequestParam(defaultValue = "abertos") String voltar,
                                     Model model, RedirectAttributes redirect) {
        Emprestimo e = emprestimos.findById(id).orElse(null);
        if (e == null || !e.isAberto()) {
            redirect.addFlashAttribute("erro", e == null ? "Empréstimo não encontrado." : "Este empréstimo já foi devolvido.");
            return "redirect:/emprestimos";
        }
        model.addAttribute("e", e);
        model.addAttribute("voltar", voltar.matches("[a-z]+|item") ? voltar : "abertos");
        return "emprestimos/devolver";
    }

    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id, @RequestParam(defaultValue = "abertos") String voltar,
                           @RequestParam(defaultValue = "") String condicao,
                           Principal principal, RedirectAttributes redirect) {
        Long itemId = null;
        try {
            Emprestimo e = estoque.registrarDevolucao(id, usuarioAtual.nome(principal), condicao);
            itemId = e.getItem().getId();
            redirect.addFlashAttribute("sucesso", "Devolução registrada: " + e.getItem().getNome()
                    + " voltou ao estoque.");
        } catch (EstoqueException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        if ("item".equals(voltar) && itemId != null) return "redirect:/itens/" + itemId;
        return "redirect:/emprestimos?status=" + (voltar.matches("[a-z]+") ? voltar : "abertos");
    }
}
