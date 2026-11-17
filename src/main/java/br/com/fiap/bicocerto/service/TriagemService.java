package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.model.Categoria;
import br.com.fiap.bicocerto.model.OrigemTriagem;
import br.com.fiap.bicocerto.model.Urgencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Triagem inteligente da solicitacao com Spring AI.
 * <p>
 * O modelo recebe as categorias cadastradas no banco (contexto) e exemplos de resposta (few-shot no prompt), e devolve um
 * JSON estruturado com categoria, urgencia, complexidade, resumo e dica de seguranca. A resposta nunca e usada
 * "crua": a categoria precisa existir no banco e o preco justo e calculado pelo sistema sobre a faixa de referencia
 * da categoria, de acordo com a complexidade. Se o modelo estiver fora do ar ou responder algo invalido, entra a
 * triagem por regras (palavras-chave), para o app continuar funcionando.
 * <p>
 * Com {@code IA_SERVICO_URL} configurada, a triagem passa primeiro pelo servico de IA em Python (repositorio
 * bico-certo-ia): classificador de ML + RAG sobre o historico de servicos do bairro + orientacao gerada por LLM.
 * Se ele falhar ou nao reconhecer o problema, segue o fluxo acima (Spring AI e depois regras).
 */
@Service
public class TriagemService {

    private static final Logger log = LoggerFactory.getLogger(TriagemService.class);

    /** Palavras que indicam risco imediato: usadas na contingencia e para nunca rebaixar uma urgencia real. */
    private static final Pattern SINAIS_DE_RISCO = Pattern.compile(
            "\\b(faisca\\w*|choque\\w*|queimad\\w*|fumaca|fio exposto|fio desencapado|curto[- ]circuito|gas|"
                    + "alagand\\w*|alagad\\w*|inundand\\w*|estourou)\\b");

    /** Palavras-chave extras por categoria para a triagem de contingencia (alem da descricao da categoria). */
    private static final Map<String, List<String>> PALAVRAS_CHAVE = Map.of(
            "eletrica", List.of("luz", "tomada", "disjuntor", "chuveiro", "fio", "lampada", "energia", "interruptor",
                    "faisca", "choque", "curto", "ventilador"),
            "encanamento", List.of("vaza", "vazamento", "cano", "torneira", "pia", "ralo", "entupi", "descarga",
                    "registro", "caixa d", "agua", "esgoto"),
            "limpeza", List.of("faxina", "limpeza", "limpar", "sujeira", "pos-obra", "pos obra", "vidro", "sofa"),
            "pintura", List.of("pintar", "pintura", "parede", "tinta", "mofo", "massa corrida", "portao"),
            "montagem de moveis", List.of("montar", "montagem", "guarda-roupa", "guarda roupa", "armario", "cama",
                    "rack", "estante", "movel", "moveis"),
            "pequenos reparos", List.of("prateleira", "cortina", "suporte", "tv", "fechadura", "porta", "reparo",
                    "furo", "parafuso"),
            "pedreiro", List.of("pedreiro", "reboco", "piso", "azulejo", "rejunte", "contrapiso", "cimento", "muro",
                    "rachadura", "trinca", "tijolo", "laje", "alvenaria", "reforma"),
            "marceneiro", List.of("marceneiro", "marcenaria", "madeira", "empenad", "gaveta", "dobradica", "verniz",
                    "sob medida", "mesa", "cadeira"),
            "mecanico", List.of("mecanico", "carro", "moto", "motor", "bateria", "oleo", "freio", "pneu",
                    "embreagem", "radiador", "nao pega"));

    private final ChatClient chatClient;
    private final CategoriaService categoriaService;
    private final String instrucaoSistema;
    /** Cliente do servico de IA em Python; null quando IA_SERVICO_URL nao esta configurada. */
    private final RestClient servicoIa;

    public TriagemService(ChatClient.Builder chatClientBuilder, CategoriaService categoriaService,
                          @Value("classpath:prompts/triagem.st") Resource instrucaoSistema,
                          @Value("${bicocerto.ia-servico.url:}") String urlServicoIa) {
        this.chatClient = chatClientBuilder.build();
        this.categoriaService = categoriaService;
        this.instrucaoSistema = ler(instrucaoSistema);
        this.servicoIa = urlServicoIa == null || urlServicoIa.isBlank() ? null : criarClienteServicoIa(urlServicoIa);
    }

    /** Resposta do servico de IA em Python (POST /triagem). */
    record RespostaServicoIa(String categoria, FaixaServicoIa faixa, String orientacao,
                             @JsonProperty("aviso_seguranca") String avisoSeguranca,
                             @JsonProperty("origem_orientacao") String origemOrientacao) {
    }

    record FaixaServicoIa(int minimo, int maximo, int mediana, int amostras) {
    }

    /** Resposta estruturada esperada do modelo (Spring AI converte o JSON neste record). */
    record TriagemIa(String categoria, String urgencia, String complexidade, String resumo, String dicaSeguranca) {
    }

    /** Complexidade do servico: define em que parte da faixa de referencia da categoria o preco justo fica. */
    enum Complexidade {
        SIMPLES(0.00, 0.40),
        MEDIA(0.25, 0.65),
        COMPLEXA(0.55, 1.00);

        private final double inicio;
        private final double fim;

        Complexidade(double inicio, double fim) {
            this.inicio = inicio;
            this.fim = fim;
        }

        static Complexidade de(String valor) {
            try {
                return valor == null ? MEDIA : valueOf(semAcento(valor).toUpperCase(Locale.ROOT).trim());
            } catch (IllegalArgumentException ex) {
                return MEDIA;
            }
        }
    }

    public ResultadoTriagem triar(String descricao) {
        List<Categoria> categorias = categoriaService.todas();
        if (categorias.isEmpty()) {
            throw new RegraNegocioException("Nenhuma categoria cadastrada para fazer a triagem");
        }
        if (servicoIa != null) {
            Optional<ResultadoTriagem> peloServico = triarPeloServicoIa(descricao, categorias);
            if (peloServico.isPresent()) {
                return peloServico.get();
            }
        }
        try {
            TriagemIa resposta = chatClient.prompt()
                    .system(instrucaoSistema.replace("{categorias}", tabelaDeReferencia(categorias)))
                    .user("Problema descrito pelo morador: " + descricao)
                    .call()
                    .entity(TriagemIa.class);
            Optional<ResultadoTriagem> resultado = normalizar(resposta, categorias, descricao);
            if (resultado.isPresent()) {
                return resultado.get();
            }
            log.warn("Resposta do modelo fora da tabela de referencia, usando regras: {}", resposta);
        } catch (RuntimeException ex) {
            log.warn("Modelo de IA indisponivel ({}), usando triagem por regras", ex.getMessage());
        }
        return triarPorRegras(descricao, categorias);
    }

    private Optional<ResultadoTriagem> triarPeloServicoIa(String descricao, List<Categoria> categorias) {
        try {
            RespostaServicoIa resposta = servicoIa.post().uri("/triagem")
                    .body(Map.of("descricao", descricao))
                    .retrieve()
                    .body(RespostaServicoIa.class);
            Optional<ResultadoTriagem> resultado = deServicoIa(resposta, categorias, descricao);
            if (resultado.isEmpty()) {
                log.info("Servico de IA nao reconheceu o problema, seguindo para o Spring AI");
            }
            return resultado;
        } catch (RuntimeException ex) {
            log.warn("Servico de IA indisponivel ({}), seguindo para o Spring AI", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Valida a resposta do servico de IA: a categoria precisa existir no banco e a faixa do RAG e limitada a faixa
     * de referencia da categoria (guardrail). Vazio = servico nao reconheceu o problema.
     */
    static Optional<ResultadoTriagem> deServicoIa(RespostaServicoIa resposta, List<Categoria> categorias,
                                                  String descricao) {
        if (resposta == null || resposta.categoria() == null || resposta.faixa() == null) {
            return Optional.empty();
        }
        Optional<Categoria> categoria = categorias.stream()
                .filter(c -> semAcento(c.getNome()).equals(semAcento(resposta.categoria())))
                .findFirst();
        if (categoria.isEmpty()) {
            return Optional.empty();
        }
        Categoria cat = categoria.get();
        BigDecimal minimo = limitar(BigDecimal.valueOf(resposta.faixa().minimo()), cat);
        BigDecimal maximo = limitar(BigDecimal.valueOf(resposta.faixa().maximo()), cat);
        if (minimo.compareTo(maximo) >= 0) {
            return Optional.empty();
        }
        Urgencia urgencia = temSinalDeRisco(descricao) ? Urgencia.ALTA : Urgencia.MEDIA;
        String servico = "Serviço de " + cat.getNome().toLowerCase(Locale.ROOT);
        String dica = textoOuPadrao(resposta.avisoSeguranca(), dicaPadrao(cat, urgencia));
        return Optional.of(new ResultadoTriagem(cat, urgencia, minimo, maximo,
                textoOuPadrao(resposta.orientacao(), servico), dica, OrigemTriagem.IA));
    }

    private static BigDecimal limitar(BigDecimal valor, Categoria categoria) {
        return valor.max(categoria.getPrecoMinimo()).min(categoria.getPrecoMaximo()).setScale(2, RoundingMode.HALF_UP);
    }

    private static RestClient criarClienteServicoIa(String url) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(45));
        return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    /** Valida e ajusta a resposta do modelo. Vazio = resposta inutilizavel (categoria inexistente). */
    static Optional<ResultadoTriagem> normalizar(TriagemIa ia, List<Categoria> categorias, String descricao) {
        if (ia == null || ia.categoria() == null) {
            return Optional.empty();
        }
        Optional<Categoria> categoria = categorias.stream()
                .filter(c -> semAcento(c.getNome()).equals(semAcento(ia.categoria())))
                .findFirst();
        if (categoria.isEmpty()) {
            return Optional.empty();
        }
        Categoria cat = categoria.get();

        // guardrail: se nenhuma palavra da descricao combina com a categoria da IA, mas combina com outra, corrige
        Map<Categoria, Integer> pontos = pontuarCategorias(descricao, categorias);
        Categoria melhorPorRegra = melhorCategoria(pontos, categorias);
        boolean corrigida = pontos.get(cat) == 0 && pontos.get(melhorPorRegra) > 0;
        if (corrigida) {
            log.info("Guardrail: categoria '{}' sugerida pelo modelo corrigida para '{}'", cat.getNome(),
                    melhorPorRegra.getNome());
            cat = melhorPorRegra;
        }

        // o preco nunca vem do modelo: e calculado sobre a faixa de referencia da categoria
        Complexidade complexidade = Complexidade.de(ia.complexidade());
        BigDecimal minimo = pontoDaFaixa(cat, complexidade.inicio);
        BigDecimal maximo = pontoDaFaixa(cat, complexidade.fim);

        // guardrail de urgencia: ALTA exige sinal real de risco; sinal de risco sempre vira ALTA
        Urgencia urgencia = converterUrgencia(ia.urgencia());
        boolean risco = temSinalDeRisco(descricao);
        if (risco) {
            urgencia = Urgencia.ALTA;
        } else if (urgencia == Urgencia.ALTA) {
            urgencia = Urgencia.MEDIA;
        }

        String servico = "Serviço de " + cat.getNome().toLowerCase(Locale.ROOT);
        String resumo = corrigida ? servico : textoOuPadrao(ia.resumo(), servico);
        String dica = corrigida ? dicaPadrao(cat, urgencia) : textoOuPadrao(ia.dicaSeguranca(), dicaPadrao(cat, urgencia));
        return Optional.of(new ResultadoTriagem(cat, urgencia, minimo, maximo, resumo, dica, OrigemTriagem.IA));
    }

    /** Contingencia sem IA: escolhe a categoria com mais palavras-chave encontradas na descricao. */
    static ResultadoTriagem triarPorRegras(String descricao, List<Categoria> categorias) {
        Categoria melhor = melhorCategoria(pontuarCategorias(descricao, categorias), categorias);
        Urgencia urgencia = temSinalDeRisco(descricao) ? Urgencia.ALTA : Urgencia.MEDIA;
        return new ResultadoTriagem(melhor, urgencia, melhor.getPrecoMinimo(), melhor.getPrecoMaximo(),
                "Serviço de " + melhor.getNome().toLowerCase(Locale.ROOT) + " (triagem automática por regras)",
                dicaPadrao(melhor, urgencia), OrigemTriagem.REGRAS);
    }

    /** Quantas palavras-chave de cada categoria aparecem na descricao. */
    static Map<Categoria, Integer> pontuarCategorias(String descricao, List<Categoria> categorias) {
        String texto = semAcento(descricao);
        Map<Categoria, Integer> pontos = new LinkedHashMap<>();
        for (Categoria categoria : categorias) {
            List<String> palavras = PALAVRAS_CHAVE.getOrDefault(semAcento(categoria.getNome()), List.of());
            pontos.put(categoria, (int) palavras.stream().filter(texto::contains).count());
        }
        return pontos;
    }

    private static Categoria melhorCategoria(Map<Categoria, Integer> pontos, List<Categoria> categorias) {
        return pontos.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(categorias.get(0));
    }

    static boolean temSinalDeRisco(String descricao) {
        return SINAIS_DE_RISCO.matcher(semAcento(descricao)).find();
    }

    /** Valor na posicao (0.0 = minimo, 1.0 = maximo) da faixa de referencia, arredondado para multiplos de R$ 5. */
    static BigDecimal pontoDaFaixa(Categoria categoria, double posicao) {
        BigDecimal amplitude = categoria.getPrecoMaximo().subtract(categoria.getPrecoMinimo());
        BigDecimal valor = categoria.getPrecoMinimo().add(amplitude.multiply(BigDecimal.valueOf(posicao)));
        BigDecimal cinco = BigDecimal.valueOf(5);
        return valor.divide(cinco, 0, RoundingMode.HALF_UP).multiply(cinco).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static Urgencia converterUrgencia(String valor) {
        if (valor == null) {
            return Urgencia.MEDIA;
        }
        try {
            return Urgencia.valueOf(semAcento(valor).toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException ex) {
            return Urgencia.MEDIA;
        }
    }

    private static String dicaPadrao(Categoria categoria, Urgencia urgencia) {
        String nome = semAcento(categoria.getNome());
        if (nome.equals("eletrica")) {
            return "Desligue o disjuntor do cômodo e não toque em fios até o eletricista chegar.";
        }
        if (nome.equals("encanamento")) {
            return "Feche o registro de água para evitar desperdício e danos até o encanador chegar.";
        }
        return urgencia == Urgencia.ALTA
                ? "Afaste as pessoas do local do problema até o prestador chegar."
                : "Deixe o local livre e separe fotos do problema para mostrar ao prestador.";
    }

    private static String textoOuPadrao(String texto, String padrao) {
        if (texto == null || texto.isBlank()) {
            return padrao;
        }
        String limpo = texto.trim();
        return limpo.length() > 480 ? limpo.substring(0, 480) : limpo;
    }

    private static String tabelaDeReferencia(List<Categoria> categorias) {
        return categorias.stream()
                .map(c -> "- " + c.getNome() + " | " + c.getDescricao())
                .collect(Collectors.joining("\n"));
    }

    static String semAcento(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static String ler(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o prompt de triagem", e);
        }
    }
}
