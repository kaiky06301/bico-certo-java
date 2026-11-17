package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Notificacao;
import br.com.fiap.bicocerto.model.TipoAviso;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotificacaoResponse(Long id, Long solicitacaoId, TipoAviso tipo, String titulo, String mensagem,
                                  BigDecimal valor, Long orcamentoId, boolean lida, LocalDateTime criadaEm) {

    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getSolicitacao().getId(), n.getTipo(), n.getTipo().getTitulo(),
                n.getMensagem(), n.getValor(), n.getOrcamentoId(), n.isLida(), n.getCriadaEm());
    }
}
