package br.com.fiap.bicocerto.messaging;

import java.time.LocalDateTime;

/** Mensagem publicada no RabbitMQ a cada mudanca importante de uma solicitacao. */
public record SolicitacaoEvento(
        TipoEvento tipo,
        Long solicitacaoId,
        Long categoriaId,
        String bairro,
        String urgencia,
        String resumo,
        LocalDateTime ocorridoEm) {
}
