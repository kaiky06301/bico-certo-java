package br.com.fiap.bicocerto.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Le o header "Authorization: Bearer <token>" e autentica o usuario da requisicao. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UsuarioDetailsService usuarioDetailsService) {
        this.jwtUtil = jwtUtil;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtUtil.extrairEmail(header.substring(7));
                UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);
                var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            } catch (Exception ex) {
                // token invalido/expirado: segue sem autenticar e o Security responde 401
                log.debug("JWT rejeitado: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
