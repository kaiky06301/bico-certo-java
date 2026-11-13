package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CategoriaRequest(
        @NotBlank(message = "Informe o nome") @Size(max = 60) String nome,
        @NotBlank(message = "Informe a descricao") @Size(max = 500) String descricao,
        @NotNull @DecimalMin(value = "1.00", message = "Preco minimo deve ser positivo") BigDecimal precoMinimo,
        @NotNull @DecimalMin(value = "1.00", message = "Preco maximo deve ser positivo") BigDecimal precoMaximo) {
}
