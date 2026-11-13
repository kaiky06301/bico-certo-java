package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String telefone, String bairro, Papel papel) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getBairro(), u.getPapel());
    }
}
