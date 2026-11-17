package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;

/** Exclusao de conta pede a senha de novo, para ninguem apagar a conta de outra pessoa com o celular desbloqueado. */
public record ExcluirContaRequest(@NotBlank(message = "Confirme sua senha") String senha) {
}
