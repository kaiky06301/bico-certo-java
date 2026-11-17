package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.model.Categoria;
import br.com.fiap.bicocerto.model.OrigemTriagem;
import br.com.fiap.bicocerto.model.Urgencia;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras de negocio da triagem que validam e corrigem a resposta do modelo de IA. */
class TriagemServiceTest {

    private final Categoria eletrica = new Categoria("Eletrica", "Tomadas e disjuntores",
            new BigDecimal("80.00"), new BigDecimal("350.00"));
    private final Categoria encanamento = new Categoria("Encanamento", "Vazamentos e entupimentos",
            new BigDecimal("70.00"), new BigDecimal("400.00"));
    private final Categoria pequenosReparos = new Categoria("Pequenos reparos", "Prateleiras e suportes",
            new BigDecimal("60.00"), new BigDecimal("250.00"));
    private final List<Categoria> categorias = List.of(eletrica, encanamento, pequenosReparos);

    @Test
    void respostaValidaDoModeloUsaComplexidadeParaCalcularOPreco() {
        var ia = new TriagemService.TriagemIa("Eletrica", "MEDIA", "SIMPLES",
                "Tomada da sala sem funcionar", "Desligue o disjuntor.");

        Optional<ResultadoTriagem> resultado = TriagemService.normalizar(ia, categorias, "A tomada da sala parou");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().categoria()).isSameAs(eletrica);
        assertThat(resultado.get().urgencia()).isEqualTo(Urgencia.MEDIA);
        assertThat(resultado.get().precoMinimo()).isEqualByComparingTo("80.00");
        assertThat(resultado.get().precoMaximo()).isEqualByComparingTo("190.00");
        assertThat(resultado.get().origem()).isEqualTo(OrigemTriagem.IA);
    }

    @Test
    void categoriaComAcentoEMaiusculaTambemEReconhecida() {
        var ia = new TriagemService.TriagemIa("ELÉTRICA", "BAIXA", "COMPLEXA", "Refazer fiacao", "Dica");

        Optional<ResultadoTriagem> resultado = TriagemService.normalizar(ia, categorias, "Quero refazer a fiacao da casa");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().categoria()).isSameAs(eletrica);
        assertThat(resultado.get().precoMaximo()).isEqualByComparingTo("350.00");
    }

    @Test
    void categoriaInexistenteEDescartada() {
        var ia = new TriagemService.TriagemIa("Jardinagem", "BAIXA", "SIMPLES", "Cortar grama", "Dica");

        assertThat(TriagemService.normalizar(ia, categorias, "Cortar a grama")).isEmpty();
        assertThat(TriagemService.normalizar(null, categorias, "qualquer")).isEmpty();
    }

    @Test
    void guardrailCorrigeCategoriaSemRelacaoComADescricao() {
        var ia = new TriagemService.TriagemIa("Eletrica", "BAIXA", "SIMPLES", "Resumo errado", "Dica errada");

        ResultadoTriagem resultado = TriagemService.normalizar(ia, categorias,
                "A pia da cozinha entupiu e a agua nao desce").orElseThrow();

        assertThat(resultado.categoria()).isSameAs(encanamento);
        assertThat(resultado.resumo()).isEqualTo("Serviço de encanamento");
    }

    @Test
    void sinalDeRiscoSempreViraUrgenciaAlta() {
        var ia = new TriagemService.TriagemIa("Eletrica", "BAIXA", "SIMPLES", "Tomada", "Dica");

        ResultadoTriagem resultado = TriagemService.normalizar(ia, categorias,
                "A tomada esta soltando faisca").orElseThrow();

        assertThat(resultado.urgencia()).isEqualTo(Urgencia.ALTA);
    }

    @Test
    void urgenciaAltaSemSinalDeRiscoeRebaixadaParaMedia() {
        var ia = new TriagemService.TriagemIa("Pequenos reparos", "ALTA", "SIMPLES", "Suporte de TV", "Dica");

        ResultadoTriagem resultado = TriagemService.normalizar(ia, categorias,
                "Quero instalar um suporte de TV na sala").orElseThrow();

        assertThat(resultado.urgencia()).isEqualTo(Urgencia.MEDIA);
    }

    @Test
    void triagemPorRegrasFuncionaSemIa() {
        ResultadoTriagem resultado = TriagemService.triarPorRegras(
                "Estourou o cano da descarga e esta alagando o banheiro", categorias);

        assertThat(resultado.categoria()).isSameAs(encanamento);
        assertThat(resultado.urgencia()).isEqualTo(Urgencia.ALTA);
        assertThat(resultado.origem()).isEqualTo(OrigemTriagem.REGRAS);
        assertThat(resultado.precoMinimo()).isEqualByComparingTo("70.00");
        assertThat(resultado.precoMaximo()).isEqualByComparingTo("400.00");
    }

    @Test
    void palavrasParecidasNaoDisparamFalsoRisco() {
        assertThat(TriagemService.temSinalDeRisco("Gastei muito com a conta de luz")).isFalse();
        assertThat(TriagemService.temSinalDeRisco("Sinto cheiro de gás na cozinha")).isTrue();
    }

    @Test
    void pontoDaFaixaArredondaParaMultiploDeCinco() {
        assertThat(TriagemService.pontoDaFaixa(eletrica, 0.0)).isEqualByComparingTo("80.00");
        assertThat(TriagemService.pontoDaFaixa(eletrica, 0.4)).isEqualByComparingTo("190.00");
        assertThat(TriagemService.pontoDaFaixa(eletrica, 1.0)).isEqualByComparingTo("350.00");
    }

    @Test
    void triagemPorRegrasReconheceOsServicosNovos() {
        Categoria mecanico = new Categoria("Mecânico", "Carro ou moto", new java.math.BigDecimal("80.00"), new java.math.BigDecimal("600.00"));
        Categoria pedreiro = new Categoria("Pedreiro", "Reboco e piso", new java.math.BigDecimal("150.00"), new java.math.BigDecimal("1500.00"));
        List<Categoria> todas = List.of(eletrica, encanamento, pequenosReparos, mecanico, pedreiro);

        assertThat(TriagemService.triarPorRegras("Meu carro não pega, acho que é a bateria", todas).categoria())
                .isSameAs(mecanico);
        assertThat(TriagemService.triarPorRegras("Preciso assentar piso e fazer o reboco do muro", todas).categoria())
                .isSameAs(pedreiro);
    }

    @Test
    void respostaDoServicoDeIaUsaFaixaDoRagLimitadaAReferencia() {
        var resposta = new TriagemService.RespostaServicoIa("Encanamento",
                new TriagemService.FaixaServicoIa(40, 160, 120, 5),
                "Parece entupimento. Vizinhos pagaram entre R$ 40 e R$ 160.", null, "llm");

        Optional<ResultadoTriagem> resultado = TriagemService.deServicoIa(resposta, categorias, "pia entupida");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().categoria()).isSameAs(encanamento);
        assertThat(resultado.get().precoMinimo()).isEqualByComparingTo("70.00"); // 40 subiu para o minimo da categoria
        assertThat(resultado.get().precoMaximo()).isEqualByComparingTo("160.00");
        assertThat(resultado.get().resumo()).startsWith("Parece entupimento");
        assertThat(resultado.get().origem()).isEqualTo(OrigemTriagem.IA);
    }

    @Test
    void servicoDeIaSemCategoriaOuComCategoriaDesconhecidaEhIgnorado() {
        var foraDoEscopo = new TriagemService.RespostaServicoIa(null, null, "Nao identificado", null, "fora-do-escopo");
        var desconhecida = new TriagemService.RespostaServicoIa("Jardinagem",
                new TriagemService.FaixaServicoIa(50, 90, 70, 4), "Cortar grama", null, "llm");

        assertThat(TriagemService.deServicoIa(foraDoEscopo, categorias, "pizza")).isEmpty();
        assertThat(TriagemService.deServicoIa(desconhecida, categorias, "grama alta")).isEmpty();
    }
}
