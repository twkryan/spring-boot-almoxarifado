package br.com.almoxarifado.web;

import br.com.almoxarifado.domain.Usuario;
import br.com.almoxarifado.repository.UsuarioRepository;
import org.springframework.stereotype.Component;

import java.security.Principal;

/** Resolve o nome de quem está registrando a operação, gravado nas movimentações. */
@Component
public class UsuarioAtual {

    private final UsuarioRepository usuarios;

    public UsuarioAtual(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public String nome(Principal principal) {
        return usuarios.findByEmailIgnoreCase(principal.getName())
                .map(Usuario::getNome)
                .orElse(principal.getName());
    }
}
