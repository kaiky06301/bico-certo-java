package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O cliente descreve o problema com as proprias palavras; a triagem define categoria, urgencia e preco. */
public record SolicitacaoRequest(
        @NotBlank(message = "Descreva o problema")
        @Size(min = 10, max = 1000, message = "A descricao deve ter entre 10 e 1000 caracteres") String descricao,
        @Size(max = 80) String bairro) {
}
