package br.com.fiap.bicocerto.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario) {
}
