package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Foto da galeria de trabalhos (ou a foto de perfil, sem legenda). */
public record TrabalhoFotoRequest(
        @NotBlank(message = "Envie a foto")
        @Pattern(regexp = "^data:image/(jpeg|png);base64,[A-Za-z0-9+/=]+$", message = "A foto deve ser JPEG ou PNG")
        @Size(max = 3_000_000, message = "A foto deve ter até 2 MB") String imagem,
        @Size(max = 80, message = "A legenda deve ter até 80 caracteres") String legenda) {
}
