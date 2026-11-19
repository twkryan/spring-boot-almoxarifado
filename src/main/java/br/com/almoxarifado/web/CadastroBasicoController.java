package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.Setor;
import br.com.almoxarifado.domain.Tecnico;
import br.com.almoxarifado.repository.EmprestimoRepository;
import br.com.almoxarifado.repository.SetorRepository;
import br.com.almoxarifado.repository.TecnicoRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cadastros de apoio: técnicos e setores. */
@Controller
public class CadastroBasicoController {

    private final TecnicoRepository tecnicos;
    private final SetorRepository setores;
    private final EmprestimoRepository emprestimos;

    public CadastroBasicoController(TecnicoRepository tecnicos, SetorRepository setores, EmprestimoRepository emprestimos) {
        this.tecnicos = tecnicos;
        this.setores = setores;
        this.emprestimos = emprestimos;
    }

    // ---------------------------------------------------------------- técnicos

    @GetMapping("/tecnicos")
    public String tecnicos(@RequestParam(required = false) Long editar, Model model) {
        Tecnico tecnico = editar == null ? new Tecnico() : tecnicos.findById(editar).orElseGet(Tecnico::new);
        return telaTecnicos(tecnico, model);
    }

    @PostMapping("/tecnicos")
    public String salvarTecnico(@Valid @ModelAttribute("tecnico") Tecnico form, BindingResult result,
                                @RequestParam(required = false) Long setorId, Model model,
                                RedirectAttributes redirect) {
        form.setSetor(setorId == null ? null : setores.findById(setorId).orElse(null));
        if (form.getSetor() == null) {
            result.rejectValue("setor", "setor.obrigatorio", "Escolha o setor do técnico.");
        }
        if (form.getMatricula() != null) {
            form.setMatricula(form.getMatricula().trim().toUpperCase());
            tecnicos.findByMatriculaIgnoreCase(form.getMatricula())
                    .filter(outro -> !outro.getId().equals(form.getId()))
                    .ifPresent(outro -> result.rejectValue("matricula", "matricula.duplicada",
                            "Matrícula já usada por " + outro.getNome() + "."));
        }
        if (result.hasErrors()) return telaTecnicos(form, model);

        Tecnico tecnico = form.getId() == null ? new Tecnico() : tecnicos.findById(form.getId()).orElseGet(Tecnico::new);
        tecnico.setNome(form.getNome().trim());
        tecnico.setMatricula(form.getMatricula());
        tecnico.setEmail(form.getEmail());
        tecnico.setSetor(form.getSetor());
        tecnicos.save(tecnico);
        redirect.addFlashAttribute("sucesso", (form.getId() == null ? "Técnico cadastrado: " : "Dados atualizados: ")
                + tecnico.getNome() + ".");
        return "redirect:/tecnicos";
    }

    @PostMapping("/tecnicos/{id}/status")
    public String alternarStatus(@PathVariable Long id, RedirectAttributes redirect) {
        tecnicos.findById(id).ifPresent(t -> {
            t.setAtivo(!t.isAtivo());
            tecnicos.save(t);
            redirect.addFlashAttribute("sucesso", t.getNome() + (t.isAtivo() ? " foi reativado." : " foi desativado."));
        });
        return "redirect:/tecnicos";
    }

    private String telaTecnicos(Tecnico tecnico, Model model) {
        Map<Long, Long> abertos = new HashMap<>();
        for (Object[] linha : emprestimos.abertosPorTecnico()) {
            abertos.put((Long) linha[0], (Long) linha[1]);
        }
        List<Tecnico> lista = tecnicos.findAllByOrderByAtivoDescNomeAsc();
        model.addAttribute("tecnicos", lista);
        model.addAttribute("ativos", lista.stream().filter(Tecnico::isAtivo).count());
        model.addAttribute("abertos", abertos);
        model.addAttribute("tecnico", tecnico);
        model.addAttribute("setores", setores.findAllByOrderByNomeAsc());
        return "cadastros/tecnicos";
    }

    // ---------------------------------------------------------------- setores

    @GetMapping("/setores")
    public String setores(@RequestParam(required = false) Long editar, Model model) {
        Setor setor = editar == null ? new Setor() : setores.findById(editar).orElseGet(Setor::new);
        return telaSetores(setor, model);
    }

    @PostMapping("/setores")
    public String salvarSetor(@Valid @ModelAttribute("setor") Setor form, BindingResult result, Model model,
                              RedirectAttributes redirect) {
        if (form.getNome() != null && !form.getNome().isBlank()) {
            setores.findAllByOrderByNomeAsc().stream()
                    .filter(s -> s.getNome().trim().equalsIgnoreCase(form.getNome().trim()) && !s.getId().equals(form.getId()))
                    .findFirst()
                    .ifPresent(s -> result.rejectValue("nome", "nome.duplicado", "Já existe um setor com este nome."));
        }
        if (result.hasErrors()) return telaSetores(form, model);
        Setor setor = form.getId() == null ? new Setor() : setores.findById(form.getId()).orElseGet(Setor::new);
        setor.setNome(form.getNome().trim());
        setor.setCentroCusto(form.getCentroCusto() == null || form.getCentroCusto().isBlank() ? null : form.getCentroCusto().trim().toUpperCase());
        setor.setResponsavel(form.getResponsavel());
        setores.save(setor);
        redirect.addFlashAttribute("sucesso", (form.getId() == null ? "Setor cadastrado: " : "Setor atualizado: ")
                + setor.getNome() + ".");
        return "redirect:/setores";
    }

    private String telaSetores(Setor setor, Model model) {
        List<Setor> lista = setores.findAllByOrderByNomeAsc();
        Map<Long, Long> equipe = new HashMap<>();
        for (Setor s : lista) {
            equipe.put(s.getId(), tecnicos.contarPorSetor(s.getId()));
        }
        model.addAttribute("setores", lista);
        model.addAttribute("equipe", equipe);
        model.addAttribute("setor", setor);
        return "cadastros/setores";
    }
}
