package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Categoria;

import java.math.BigDecimal;

public record CategoriaResponse(Long id, String nome, String descricao, BigDecimal precoMinimo, BigDecimal precoMaximo) {

    public static CategoriaResponse from(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao(), c.getPrecoMinimo(), c.getPrecoMaximo());
    }
}
