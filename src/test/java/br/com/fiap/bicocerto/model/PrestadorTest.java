package br.com.fiap.bicocerto.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PrestadorTest {

    @Test
    void mediaDaReputacaoEAtualizadaACadaAvaliacao() {
        Prestador prestador = new Prestador(null, null, "Eletricista");

        prestador.registrarAvaliacao(5);
        prestador.registrarAvaliacao(4);
        prestador.registrarAvaliacao(4);

        assertThat(prestador.getTotalAvaliacoes()).isEqualTo(3);
        assertThat(prestador.getNotaMedia()).isEqualByComparingTo(new BigDecimal("4.33"));
    }
}
