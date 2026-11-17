package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String nome, String email, String telefone, String bairro,
                              Papel papel, boolean ativo, LocalDateTime criadoEm) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getBairro(),
                u.getPapel(), u.isAtivo(), u.getCriadoEm());
    }
}
