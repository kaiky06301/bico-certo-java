package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Perfil profissional. categoriaId = servico principal; outrasCategoriaIds = outros servicos que tambem faz (sem limite).
 * diasAtendimento usa SEG, TER, QUA, QUI, SEX, SAB, DOM; horarios no formato HH:mm.
 */
public record PrestadorPerfilRequest(
        @NotNull(message = "Informe a categoria") Long categoriaId,
        @Size(max = 500, message = "A descrição deve ter até 500 caracteres") String descricao,
        @Size(max = 200, message = "A região deve ter até 200 caracteres") String regiaoAtendimento,
        @Size(max = 200, message = "A disponibilidade deve ter até 200 caracteres") String disponibilidade,
        @NotBlank(message = "Informe o bairro") @Size(max = 80) String bairro,
        @Pattern(regexp = "^[0-9()+\\- ]{8,20}$", message = "Telefone inválido") String telefone,
        List<Long> outrasCategoriaIds,
        @Min(value = 0, message = "Tempo de experiência inválido") @Max(value = 60, message = "Tempo de experiência inválido")
        Integer anosExperiencia,
        @Size(max = 200, message = "As especialidades devem ter até 200 caracteres") String especialidades,
        @Size(max = 300, message = "Os diferenciais devem ter até 300 caracteres") String diferenciais,
        List<@Pattern(regexp = "^(SEG|TER|QUA|QUI|SEX|SAB|DOM)$", message = "Dia de atendimento inválido") String> diasAtendimento,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de início inválido") String horarioInicio,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de término inválido") String horarioFim,
        Boolean atendeEmergencia) {
}
