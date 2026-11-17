package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.TipoAviso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Aviso enviado pelo modulo .NET (orcamento, negociacao, contratacao...) para a outra parte do pedido.
 * O .NET repassa o token de quem fez a acao; a API Java confere se essa pessoa e o destinatario participam do pedido.
 */
public record AvisoRequest(
        @NotNull(message = "Informe o tipo do aviso") TipoAviso tipo,
        @NotNull(message = "Informe o destinatario") Long destinatarioId,
        @NotBlank(message = "Informe a mensagem") @Size(max = 300) String mensagem,
        BigDecimal valor,
        Long orcamentoId) {
}
