package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tela de perfil: minha conta, troca de senha, exclusao, perfil profissional e selo de verificacao. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContaIntegracaoTest {

    private static final String SENHA = "senha-teste-123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PrestadorRepository prestadorRepository;
    @Autowired private SolicitacaoRepository solicitacaoRepository;
    @Autowired private AvaliacaoRepository avaliacaoRepository;
    @Autowired private NotificacaoRepository notificacaoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private ChatModel chatModel;
    @MockitoBean private RabbitTemplate amqpTemplate;

    private Long prestadorId;
    private Long prestadorUsuarioId;

    @BeforeEach
    void preparar() {
        notificacaoRepository.deleteAll();
        avaliacaoRepository.deleteAll();
        solicitacaoRepository.deleteAll();
        prestadorRepository.deleteAll();
        usuarioRepository.deleteAll();

        String hash = passwordEncoder.encode(SENHA);
        usuarioRepository.save(new Usuario("Maria Teste", "maria@teste.com", hash, "(11) 90000-0001", "Capao Redondo", Papel.CLIENTE));
        usuarioRepository.save(new Usuario("Admin Teste", "admin@teste.com", hash, null, "Centro", Papel.ADMIN));
        var eletrica = categoriaRepository.findAll().stream().filter(c -> c.getNome().equals("Eletrica")).findFirst().orElseThrow();
        Usuario joao = usuarioRepository.save(new Usuario("Joao", "joao@teste.com", hash, "(11) 90000-0002", "Capao Redondo", Papel.PRESTADOR));
        prestadorUsuarioId = joao.getId();
        prestadorId = prestadorRepository.save(new Prestador(joao, eletrica, "Eletricista")).getId();
    }

    @Test
    void usuarioVeEAtualizaOsProprioDados() throws Exception {
        String maria = token("maria@teste.com", SENHA);

        mockMvc.perform(get("/api/conta").header("Authorization", maria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@teste.com"))
                .andExpect(jsonPath("$.papel").value("CLIENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        mockMvc.perform(put("/api/conta").header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Souza\",\"telefone\":\"(11) 98888-7777\",\"bairro\":\"Campo Limpo\",\"endereco\":\"Rua das Flores, 120\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.endereco").value("Rua das Flores, 120"));

        mockMvc.perform(put("/api/conta").header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"telefone\":\"abc\",\"bairro\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trocaDeSenhaExigeASenhaAtual() throws Exception {
        String maria = token("maria@teste.com", SENHA);

        mockMvc.perform(put("/api/conta/senha").header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"nova-senha-456\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/api/conta/senha").header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"" + SENHA + "\",\"novaSenha\":\"nova-senha-456\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@teste.com\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
        token("maria@teste.com", "nova-senha-456");
    }

    @Test
    void excluirContaApagaOsDadosEBloqueiaOLogin() throws Exception {
        String maria = token("maria@teste.com", SENHA);

        mockMvc.perform(delete("/api/conta").header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"errada\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/conta").header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@teste.com\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
        // o token antigo tambem deixa de valer
        mockMvc.perform(get("/api/conta").header("Authorization", maria)).andExpect(status().isUnauthorized());
    }

    @Test
    void prestadorCompletaOPerfilProfissionalEClienteVePeloIdDoUsuario() throws Exception {
        String joao = token("joao@teste.com", SENHA);
        var categoria = categoriaRepository.findAll().stream().filter(c -> c.getNome().equals("Eletrica")).findFirst().orElseThrow();

        mockMvc.perform(put("/api/prestadores/me").header("Authorization", joao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + categoria.getId() + ",\"descricao\":\"12 anos de experiencia\","
                                + "\"regiaoAtendimento\":\"Capao Redondo e regiao\",\"disponibilidade\":\"Seg a sab, 8h-18h\","
                                + "\"bairro\":\"Capao Redondo\",\"telefone\":\"(11) 90000-0002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regiaoAtendimento").value("Capao Redondo e regiao"))
                .andExpect(jsonPath("$.verificado").value(false));

        mockMvc.perform(get("/api/prestadores/usuario/{id}", prestadorUsuarioId)
                        .header("Authorization", token("maria@teste.com", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prestadorId))
                .andExpect(jsonPath("$.disponibilidade").value("Seg a sab, 8h-18h"));
    }

    @Test
    void soOAdminConcedeOSeloDeVerificado() throws Exception {
        String corpo = "{\"verificado\":true}";
        mockMvc.perform(patch("/api/prestadores/{id}/verificacao", prestadorId)
                        .header("Authorization", token("joao@teste.com", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/prestadores/{id}/verificacao", prestadorId)
                        .header("Authorization", token("admin@teste.com", SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificado").value(true));
    }

    private String token(String email, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + jsonMapper.readTree(corpo).get("accessToken").asString();
    }
}
