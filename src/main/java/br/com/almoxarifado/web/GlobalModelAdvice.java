package br.com.almoxarifado.web;

import br.com.almoxarifado.repository.EmprestimoRepository;
import br.com.almoxarifado.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.time.LocalDate;

/** Dados que o layout (menu lateral) precisa em todas as páginas. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UsuarioRepository usuarios;
    private final EmprestimoRepository emprestimos;

    public GlobalModelAdvice(UsuarioRepository usuarios, EmprestimoRepository emprestimos) {
        this.usuarios = usuarios;
        this.emprestimos = emprestimos;
    }

    /** Valores em reais aceitam vírgula decimal em todos os formulários. */
    @InitBinder
    public void moeda(WebDataBinder binder) {
        binder.registerCustomEditor(java.math.BigDecimal.class, new MoedaEditor());
    }

    @ModelAttribute
    public void dadosDoLayout(Model model, HttpServletRequest request, Principal principal) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        model.addAttribute("path", path.isEmpty() ? "/" : path);
        if (principal != null) {
            usuarios.findByEmailIgnoreCase(principal.getName())
                    .ifPresent(u -> model.addAttribute("usuarioLogado", u));
            model.addAttribute("badgeAtrasados",
                    emprestimos.countByDataDevolucaoIsNullAndDataPrevistaBefore(LocalDate.now()));
        }
    }
}
