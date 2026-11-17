package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Avaliacao;

import java.time.LocalDateTime;

public record AvaliacaoResponse(Long id, Long solicitacaoId, Long prestadorId, String prestador, String cliente,
                                String servico, int nota, String comentario, LocalDateTime criadaEm) {

    public static AvaliacaoResponse from(Avaliacao a) {
        return new AvaliacaoResponse(a.getId(), a.getSolicitacao().getId(), a.getPrestador().getId(),
                a.getPrestador().getUsuario().getNome(), a.getSolicitacao().getCliente().getNome(),
                a.getSolicitacao().getCategoria().getNome(), a.getNota(), a.getComentario(), a.getCriadaEm());
    }
}
