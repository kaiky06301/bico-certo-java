package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank(message = "De um nome para o endereco (ex.: Casa)") @Size(max = 40, message = "O apelido deve ter ate 40 caracteres") String apelido,
        @NotBlank(message = "Informe a rua e o numero") @Size(max = 200, message = "A rua deve ter ate 200 caracteres") String logradouro,
        @Size(max = 100, message = "O complemento deve ter ate 100 caracteres") String complemento,
        @NotBlank(message = "Informe o bairro") @Size(max = 80, message = "O bairro deve ter ate 80 caracteres") String bairro,
        @NotBlank(message = "Informe a cidade") @Size(max = 60, message = "A cidade deve ter ate 60 caracteres") String cidade,
        @NotBlank(message = "Informe a UF") @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF invalida (ex.: SP)") String uf,
        Boolean principal) {
}
