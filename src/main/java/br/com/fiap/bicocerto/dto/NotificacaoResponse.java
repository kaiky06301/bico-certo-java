package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Notificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(Long id, Long solicitacaoId, String mensagem, boolean lida, LocalDateTime criadaEm) {

    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getSolicitacao().getId(), n.getMensagem(), n.isLida(),
                n.getCriadaEm());
    }
}
