package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.Usuario;
import br.com.almoxarifado.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import br.com.almoxarifado.web.form.CadastroForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final SecurityContextRepository contextoRepositorio = new HttpSessionSecurityContextRepository();

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login(Principal principal, Model model) {
        if (principal != null) return "redirect:/";
        model.addAttribute("semUsuarios", usuarioService.nenhumUsuario());
        return "auth/login";
    }

    /** Cadastro público. Quem já está logado cria novas contas pela tela de Usuários. */
    @GetMapping("/cadastro")
    public String cadastro(Principal principal, Model model) {
        if (principal != null) return "redirect:/usuarios";
        model.addAttribute("form", new CadastroForm());
        model.addAttribute("semUsuarios", usuarioService.nenhumUsuario());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult result,
                            Model model, RedirectAttributes redirect,
                            HttpServletRequest request, HttpServletResponse response) {
        validarCadastro(form, result, usuarioService);
        if (result.hasErrors()) {
            model.addAttribute("semUsuarios", usuarioService.nenhumUsuario());
            return "auth/cadastro";
        }
        Usuario novo = usuarioService.cadastrar(form.getNome(), form.getEmail(), form.getSenha());
        entrarAutomaticamente(novo.getEmail(), request, response);
        redirect.addFlashAttribute("sucesso", "Conta criada. Bem-vindo, " + novo.getNome().split("\\s+")[0] + "!");
        return "redirect:/";
    }

    /** Abre a sessão do usuário recém-cadastrado, sem pedir e-mail e senha de novo. */
    private void entrarAutomaticamente(String email, HttpServletRequest request, HttpServletResponse response) {
        UserDetails usuario = usuarioService.loadUserByUsername(email);
        var auth = UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(auth);
        SecurityContextHolder.setContext(contexto);
        if (request.getSession(false) != null) {
            request.changeSessionId(); // evita fixação de sessão
        }
        contextoRepositorio.saveContext(contexto, request, response);
    }

    /** Regras comuns ao cadastro público e ao cadastro feito pela tela de Usuários. */
    static void validarCadastro(CadastroForm form, BindingResult result, UsuarioService usuarioService) {
        if (form.getSenha() != null && !form.getSenha().equals(form.getConfirmacao())) {
            result.rejectValue("confirmacao", "senha.diferente", "As senhas não conferem.");
        }
        if (form.getEmail() != null && !form.getEmail().isBlank() && usuarioService.emailEmUso(form.getEmail())) {
            result.rejectValue("email", "email.em-uso", "Já existe uma conta com este e-mail.");
        }
    }
}
