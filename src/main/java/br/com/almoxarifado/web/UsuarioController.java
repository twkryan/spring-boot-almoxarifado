package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.Usuario;
import br.com.almoxarifado.service.UsuarioService;
import br.com.almoxarifado.web.form.CadastroForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Contas de acesso. Qualquer usuário logado pode criar um novo login para a equipe. */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(Model model) {
        return tela(new CadastroForm(), model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        AuthController.validarCadastro(form, result, usuarioService);
        if (result.hasErrors()) return tela(form, model);
        Usuario u = usuarioService.cadastrar(form.getNome(), form.getEmail(), form.getSenha());
        redirect.addFlashAttribute("sucesso", "Login criado para " + u.getNome() + " (" + u.getEmail() + ").");
        return "redirect:/usuarios";
    }

    private String tela(CadastroForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("usuarios", usuarioService.listar());
        return "cadastros/usuarios";
    }
}
