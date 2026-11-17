package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotNull;

/** Id do usuario do prestador cujo orcamento o cliente aceitou (o mesmo id que o modulo .NET guarda). */
public record ContratacaoRegistroRequest(@NotNull(message = "Informe o prestador contratado") Long prestadorUsuarioId) {
}
