package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.messaging.RabbitConfig;
import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fluxo completo pela API (controller -> service -> JPA/H2 com as migrations do Flyway),
 * com o modelo de IA e o RabbitMQ simulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoSolicitacaoIntegracaoTest {

    private static final String SENHA = "senha-teste-123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PrestadorRepository prestadorRepository;
    @Autowired private SolicitacaoRepository solicitacaoRepository;
    @Autowired private AvaliacaoRepository avaliacaoRepository;
    @Autowired private NotificacaoRepository notificacaoRepository;
    @Autowired private EnderecoRepository enderecoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private ChatModel chatModel;
    @MockitoBean private RabbitTemplate amqpTemplate;

    @BeforeEach
    void preparar() {
        enderecoRepository.deleteAll();
        notificacaoRepository.deleteAll();
        avaliacaoRepository.deleteAll();
        solicitacaoRepository.deleteAll();
        prestadorRepository.deleteAll();
        usuarioRepository.deleteAll();

        String hash = passwordEncoder.encode(SENHA);
        usuarioRepository.save(new Usuario("Cliente Teste", "cliente@teste.com", hash, "(11) 90000-0001", "Capao Redondo", Papel.CLIENTE));
        usuarioRepository.save(new Usuario("Admin Teste", "admin@teste.com", hash, null, "Centro", Papel.ADMIN));
        var eletrica = categoriaRepository.findAll().stream().filter(c -> c.getNome().equals("Eletrica")).findFirst().orElseThrow();
        var encanamento = categoriaRepository.findAll().stream().filter(c -> c.getNome().equals("Encanamento")).findFirst().orElseThrow();
        Usuario joao = usuarioRepository.save(new Usuario("Joao", "eletricista@teste.com", hash, "(11) 90000-0002", "Capao Redondo", Papel.PRESTADOR));
        prestadorRepository.save(new Prestador(joao, eletrica, "Eletricista"));
        Usuario carlos = usuarioRepository.save(new Usuario("Carlos", "encanador@teste.com", hash, null, "Capao Redondo", Papel.PRESTADOR));
        prestadorRepository.save(new Prestador(carlos, encanamento, "Encanador"));

        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        responderModelo("""
                {"categoria":"Eletrica","urgencia":"MEDIA","complexidade":"SIMPLES",
                 "resumo":"Tomada da sala sem energia","dicaSeguranca":"Desligue o disjuntor da sala."}""");
    }

    @Test
    void loginComSenhaErradaRetorna401Padronizado() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente@teste.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha invalidos"));
    }

    @Test
    void endpointProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/solicitacoes/minhas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token ausente, invalido ou expirado"));
    }

    @Test
    void categoriasSaoPublicasParaOCadastroDoPrestador() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    void clienteNaoPodeCriarCategoriaMasAdminPode() throws Exception {
        String corpo = "{\"nome\":\"Jardinagem\",\"descricao\":\"Poda\",\"precoMinimo\":60,\"precoMaximo\":200}";
        mockMvc.perform(post("/api/categorias").header("Authorization", token("cliente@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/categorias").header("Authorization", token("admin@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Jardinagem"));
    }

    @Test
    void validacaoDoDtoRetornaCamposInvalidos() throws Exception {
        mockMvc.perform(post("/api/solicitacoes").header("Authorization", token("cliente@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"descricao\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("descricao")));
    }

    @Test
    void listagemDeCategoriasEPaginada() throws Exception {
        mockMvc.perform(get("/api/categorias?page=0&size=2").header("Authorization", token("cliente@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    void fluxoCompletoDoPedidoAteAAvaliacao() throws Exception {
        String cliente = token("cliente@teste.com");
        String eletricista = token("eletricista@teste.com");
        String encanador = token("encanador@teste.com");

        // cliente cria o pedido: a IA (simulada) faz a triagem e o evento vai para o RabbitMQ
        String resposta = mockMvc.perform(post("/api/solicitacoes").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"A tomada da sala parou de funcionar\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria.nome").value("Eletrica"))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.precoMinimo").value(80.0))
                .andExpect(jsonPath("$.precoMaximo").value(190.0))
                .andExpect(jsonPath("$.origemTriagem").value("IA"))
                .andExpect(jsonPath("$.cliente.telefone").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = jsonMapper.readTree(resposta).get("id").asLong();
        verify(amqpTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                (Object) contains("\"tipo\":\"CRIADA\""));

        // prestador de outra categoria nao pode assumir
        mockMvc.perform(patch("/api/solicitacoes/{id}/assumir", id).header("Authorization", encanador))
                .andExpect(status().isForbidden());

        // eletricista ve o pedido aberto e assume; telefone do cliente e liberado
        mockMvc.perform(get("/api/solicitacoes/abertas").header("Authorization", eletricista))
                .andExpect(jsonPath("$.conteudo[0].id").value(id));
        mockMvc.perform(patch("/api/solicitacoes/{id}/assumir", id).header("Authorization", eletricista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.cliente.telefone").value("(11) 90000-0001"));

        // nao da para avaliar antes de concluir
        mockMvc.perform(post("/api/solicitacoes/{id}/avaliacao", id).header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nota\":5}"))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(patch("/api/solicitacoes/{id}/concluir", id).header("Authorization", eletricista))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mockMvc.perform(post("/api/solicitacoes/{id}/avaliacao", id).header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nota\":4,\"comentario\":\"Bom servico\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/prestadores?categoriaId={c}", categoriaRepository.findAll().stream()
                        .filter(c -> c.getNome().equals("Eletrica")).findFirst().orElseThrow().getId())
                        .header("Authorization", cliente))
                .andExpect(jsonPath("$.conteudo[0].notaMedia").value(4.0))
                .andExpect(jsonPath("$.conteudo[0].totalAvaliacoes").value(1));

        verify(amqpTemplate, times(3)).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), any(Object.class));
    }

    @Test
    void semIaDisponivelATriagemPorRegrasMantemOAppFuncionando() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Ollama fora do ar"));

        mockMvc.perform(post("/api/solicitacoes/triagem").header("Authorization", token("cliente@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"O cano da pia estourou e esta alagando a cozinha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.nome").value("Encanamento"))
                .andExpect(jsonPath("$.urgencia").value("ALTA"))
                .andExpect(jsonPath("$.origem").value("REGRAS"));
    }

    @Test
    void clienteNaoVePedidoDeOutroCliente() throws Exception {
        String hash = passwordEncoder.encode(SENHA);
        usuarioRepository.save(new Usuario("Outro", "outro@teste.com", hash, null, "Centro", Papel.CLIENTE));
        String resposta = mockMvc.perform(post("/api/solicitacoes").header("Authorization", token("cliente@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"A tomada da sala parou de funcionar\"}"))
                .andReturn().getResponse().getContentAsString();
        long id = jsonMapper.readTree(resposta).get("id").asLong();

        mockMvc.perform(get("/api/solicitacoes/{id}", id).header("Authorization", token("outro@teste.com")))
                .andExpect(status().isForbidden());
    }

    private void responderModelo(String json) {
        ChatResponse resposta = new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
        when(chatModel.call(any(Prompt.class))).thenReturn(resposta);
    }

    private String token(String email) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + jsonMapper.readTree(corpo).get("accessToken").asString();
    }
}
