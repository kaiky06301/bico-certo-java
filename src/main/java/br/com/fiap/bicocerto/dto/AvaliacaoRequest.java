package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequest(
        @NotNull(message = "Informe a nota") @Min(value = 1, message = "Nota minima e 1")
        @Max(value = 5, message = "Nota maxima e 5") Integer nota,
        @Size(max = 500) String comentario) {
}
