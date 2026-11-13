package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.model.Categoria;
import br.com.fiap.bicocerto.model.OrigemTriagem;
import br.com.fiap.bicocerto.model.Urgencia;

import java.math.BigDecimal;

/** Resultado normalizado da triagem, ja validado contra a tabela de referencia. */
public record ResultadoTriagem(
        Categoria categoria,
        Urgencia urgencia,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        String resumo,
        String dicaSeguranca,
        OrigemTriagem origem) {
}
