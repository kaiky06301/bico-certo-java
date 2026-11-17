package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotNull;

public record VerificacaoRequest(@NotNull(message = "Informe se o prestador esta verificado") Boolean verificado) {
}
