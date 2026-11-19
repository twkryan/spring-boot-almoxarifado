package br.com.almoxarifado.service;

import br.com.almoxarifado.domain.Perfil;
import br.com.almoxarifado.domain.Usuario;
import br.com.almoxarifado.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario u = usuarios.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(u.getEmail())
                .password(u.getSenha())
                .roles(u.getPerfil().name())
                .build();
    }

    public boolean nenhumUsuario() {
        return usuarios.count() == 0;
    }

    public java.util.List<Usuario> listar() {
        return usuarios.findAll(org.springframework.data.domain.Sort.by("nome"));
    }

    public boolean emailEmUso(String email) {
        return usuarios.existsByEmailIgnoreCase(email.trim());
    }

    /** O primeiro usuário cadastrado vira administrador; os demais, operadores. */
    @Transactional
    public Usuario cadastrar(String nome, String email, String senha) {
        Usuario u = new Usuario();
        u.setNome(nome.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setSenha(encoder.encode(senha));
        u.setPerfil(usuarios.count() == 0 ? Perfil.ADMIN : Perfil.OPERADOR);
        return usuarios.save(u);
    }
}
