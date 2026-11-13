package br.com.fiap.bicocerto.security;

import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Usuario logado disponivel nos controllers via @AuthenticationPrincipal. */
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String nome;
    private final String email;
    private final String senha;
    private final Papel papel;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.senha = usuario.getSenha();
        this.papel = usuario.getPapel();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Papel getPapel() { return papel; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() { return senha; }

    @Override
    public String getUsername() { return email; }
}
