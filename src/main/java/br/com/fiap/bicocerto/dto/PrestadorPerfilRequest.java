package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrestadorPerfilRequest(
        @NotNull(message = "Informe a categoria") Long categoriaId,
        @Size(max = 500) String descricao,
        @NotBlank(message = "Informe o bairro") @Size(max = 80) String bairro,
        @Pattern(regexp = "^[0-9()+\\- ]{8,20}$", message = "Telefone invalido") String telefone) {
}
