package br.com.fiap.bicocerto.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/** Gera e valida os tokens JWT (mesma abordagem da Aula 13, com o papel e o id do usuario nas claims). */
@Component
public class JwtUtil {

    private final Key chave;
    private final long expiracaoMs;

    public JwtUtil(@Value("${app.jwt.secret}") String segredo,
                   @Value("${app.jwt.expirationMs}") long expiracaoMs) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 caracteres");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(UsuarioAutenticado usuario) {
        Date agora = new Date();
        return Jwts.builder()
                .setSubject(usuario.getUsername())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("papel", usuario.getPapel().name())
                .setIssuer("bico-certo-api")
                .setIssuedAt(agora)
                .setExpiration(new Date(agora.getTime() + expiracaoMs))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Retorna o e-mail (subject) se o token for valido; lanca excecao se estiver expirado ou adulterado. */
    public String extrairEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public long getExpiracaoMs() {
        return expiracaoMs;
    }
}
