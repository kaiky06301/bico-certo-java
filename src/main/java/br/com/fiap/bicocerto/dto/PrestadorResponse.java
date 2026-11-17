package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Prestador;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrestadorResponse(
        Long id,
        String nome,
        String bairro,
        TriagemResponse.CategoriaResumo categoria,
        String descricao,
        String regiaoAtendimento,
        String disponibilidade,
        boolean verificado,
        BigDecimal notaMedia,
        int totalAvaliacoes,
        int servicosConcluidos,
        Long usuarioId,
        LocalDateTime membroDesde) {

    public static PrestadorResponse from(Prestador p) {
        return new PrestadorResponse(p.getId(), p.getUsuario().getNome(), p.getUsuario().getBairro(),
                new TriagemResponse.CategoriaResumo(p.getCategoria().getId(), p.getCategoria().getNome()),
                p.getDescricao(), p.getRegiaoAtendimento(), p.getDisponibilidade(), p.isVerificado(),
                p.getNotaMedia(), p.getTotalAvaliacoes(), p.getServicosConcluidos(), p.getUsuario().getId(), p.getUsuario().getCriadoEm());
    }
}
