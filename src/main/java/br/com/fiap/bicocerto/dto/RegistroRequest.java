package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Papel;
import jakarta.validation.constraints.*;

/**
 * Cadastro publico. Papel aceita CLIENTE ou PRESTADOR (ADMIN nao pode se auto cadastrar).
 * Para PRESTADOR, categoriaId e obrigatorio.
 */
public record RegistroRequest(
        @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail invalido") @Size(max = 150) String email,
        @NotBlank(message = "Informe a senha") @Size(min = 6, max = 60, message = "A senha deve ter entre 6 e 60 caracteres") String senha,
        @Pattern(regexp = "^[0-9()+\\- ]{8,20}$", message = "Telefone invalido") String telefone,
        @NotBlank(message = "Informe o bairro") @Size(max = 80) String bairro,
        @NotNull(message = "Informe o papel: CLIENTE ou PRESTADOR") Papel papel,
        Long categoriaId,
        @Size(max = 500) String descricaoServicos) {
}
