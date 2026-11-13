package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Prestador;

import java.math.BigDecimal;

public record PrestadorResponse(
        Long id,
        String nome,
        String bairro,
        TriagemResponse.CategoriaResumo categoria,
        String descricao,
        BigDecimal notaMedia,
        int totalAvaliacoes) {

    public static PrestadorResponse from(Prestador p) {
        return new PrestadorResponse(p.getId(), p.getUsuario().getNome(), p.getUsuario().getBairro(),
                new TriagemResponse.CategoriaResumo(p.getCategoria().getId(), p.getCategoria().getNome()),
                p.getDescricao(), p.getNotaMedia(), p.getTotalAvaliacoes());
    }
}
