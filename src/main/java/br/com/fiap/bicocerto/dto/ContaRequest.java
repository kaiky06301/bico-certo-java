package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados pessoais que o proprio usuario pode alterar (e-mail e papel nao mudam). */
public record ContaRequest(
        @NotBlank(message = "Informe seu nome") @Size(min = 3, max = 120, message = "O nome deve ter de 3 a 120 caracteres") String nome,
        @Pattern(regexp = "^[0-9()+\\- ]{8,20}$", message = "Telefone invalido") String telefone,
        @NotBlank(message = "Informe o bairro") @Size(max = 80, message = "O bairro deve ter ate 80 caracteres") String bairro) {
}
