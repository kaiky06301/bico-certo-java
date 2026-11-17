package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TriagemRequest(
        @NotBlank(message = "Descreva o problema")
        @Size(min = 10, max = 1000, message = "A descricao deve ter entre 10 e 1000 caracteres") String descricao,
        @Size(max = 500, message = "A observacao deve ter ate 500 caracteres") String observacao) {
}
