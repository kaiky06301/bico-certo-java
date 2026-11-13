package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.OrigemTriagem;
import br.com.fiap.bicocerto.model.Urgencia;
import br.com.fiap.bicocerto.service.ResultadoTriagem;

import java.math.BigDecimal;

public record TriagemResponse(
        CategoriaResumo categoria,
        Urgencia urgencia,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        String resumo,
        String dicaSeguranca,
        OrigemTriagem origem,
        long prestadoresDisponiveis) {

    public record CategoriaResumo(Long id, String nome) {
    }

    public static TriagemResponse from(ResultadoTriagem r, long prestadoresDisponiveis) {
        return new TriagemResponse(new CategoriaResumo(r.categoria().getId(), r.categoria().getNome()), r.urgencia(),
                r.precoMinimo(), r.precoMaximo(), r.resumo(), r.dicaSeguranca(), r.origem(), prestadoresDisponiveis);
    }
}
