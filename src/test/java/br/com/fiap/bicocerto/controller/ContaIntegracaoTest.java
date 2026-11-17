package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    @Autowired private EnderecoRepository enderecoRepository;
    @Autowired private PrestadorFotoRepository prestadorFotoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private ChatModel chatModel;
    @MockitoBean private RabbitTemplate amqpTemplate;

    private Long prestadorId;
    private Long prestadorUsuarioId;

    @BeforeEach
    void preparar() {
        enderecoRepository.deleteAll();
        prestadorFotoRepository.deleteAll();
        notificacaoRepository.deleteAll();
        avaliacaoRepository.deleteAll();
        solicitacaoRepository.deleteAll();
        prestadorRepository.deleteAll();
        usuarioRepository.deleteAll();

        String hash = passwordEncoder.encode(SENHA);
        usuarioRepository.save(new Usuario("Maria Teste", "maria@teste.com", hash, "(11) 90000-0001", "Capao Redondo", Papel.CLIENTE));
        usuarioRepository.save(new Usuario("Admin Teste", "admin@teste.com", hash, null, "Centro", Papel.ADMIN));
        var eletrica = categoriaRepository.findAll().stream().filter(c -> c.getNome().equals("Elétrica")).findFirst().orElseThrow();
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
                        .content("{\"nome\":\"Maria Souza\",\"telefone\":\"(11) 98888-7777\",\"bairro\":\"Campo Limpo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.bairro").value("Campo Limpo"));

        mockMvc.perform(put("/api/conta").header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"telefone\":\"abc\",\"bairro\":\"\",\"cidade\":\"Sao Paulo\",\"uf\":\"Sao\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enderecosSalvosEPedidoEmOutroLugar() throws Exception {
        String maria = token("maria@teste.com", SENHA);
        String base = "/api/conta/enderecos";

        // o primeiro endereco vira principal automaticamente
        mockMvc.perform(post(base).header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"Casa\",\"logradouro\":\"Rua das Flores, 120\",\"bairro\":\"Capao Redondo\",\"cidade\":\"Sao Paulo\",\"uf\":\"sp\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uf").value("SP"))
                .andExpect(jsonPath("$.principal").value(true));

        String resposta = mockMvc.perform(post(base).header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"Casa da mae\",\"logradouro\":\"Av. Brasil, 500\",\"complemento\":\"casa 2\",\"bairro\":\"Jardim Angela\",\"cidade\":\"Sao Paulo\",\"uf\":\"SP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principal").value(false))
                .andReturn().getResponse().getContentAsString();
        long casaDaMae = jsonMapper.readTree(resposta).get("id").asLong();

        mockMvc.perform(post(base).header("Authorization", maria).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"\",\"logradouro\":\"\",\"bairro\":\"\"}"))
                .andExpect(status().isBadRequest());

        // marcar outro como principal desmarca o anterior
        mockMvc.perform(patch(base + "/{id}/principal", casaDaMae).header("Authorization", maria))
                .andExpect(jsonPath("$.principal").value(true));
        mockMvc.perform(get(base).header("Authorization", maria))
                .andExpect(jsonPath("$[0].apelido").value("Casa da mae"))
                .andExpect(jsonPath("$[1].principal").value(false));

        // outro usuario nao usa o endereco da Maria
        mockMvc.perform(delete(base + "/{id}", casaDaMae).header("Authorization", token("joao@teste.com", SENHA)))
                .andExpect(status().isNotFound());

        // pedido na casa da mae: o bairro vem do endereco; o prestador nao ve a rua antes de assumir
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("IA fora do ar"));
        String pedido = mockMvc.perform(post("/api/solicitacoes").header("Authorization", maria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"A tomada da cozinha esta soltando faisca\",\"enderecoId\":" + casaDaMae + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bairro").value("Jardim Angela"))
                .andExpect(jsonPath("$.endereco").value("Av. Brasil, 500 - casa 2, Sao Paulo/SP"))
                .andReturn().getResponse().getContentAsString();
        long pedidoId = jsonMapper.readTree(pedido).get("id").asLong();

        String joao = token("joao@teste.com", SENHA);
        mockMvc.perform(get("/api/solicitacoes/{id}", pedidoId).header("Authorization", joao))
                .andExpect(jsonPath("$.endereco").doesNotExist());
        // o cliente aceita o orcamento do Joao: o pedido passa para ele e o endereco completo e liberado
        String contratacao = "{\"prestadorUsuarioId\":" + prestadorUsuarioId + "}";
        mockMvc.perform(patch("/api/solicitacoes/{id}/contratacao", pedidoId).header("Authorization", joao)
                        .contentType(MediaType.APPLICATION_JSON).content(contratacao))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/solicitacoes/{id}/contratacao", pedidoId).header("Authorization", maria)
                        .contentType(MediaType.APPLICATION_JSON).content(contratacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
        // repetir (internet instavel) nao quebra
        mockMvc.perform(patch("/api/solicitacoes/{id}/contratacao", pedidoId).header("Authorization", maria)
                        .contentType(MediaType.APPLICATION_JSON).content(contratacao))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/solicitacoes/{id}", pedidoId).header("Authorization", joao))
                .andExpect(jsonPath("$.endereco").value("Av. Brasil, 500 - casa 2, Sao Paulo/SP"));

        // excluir o endereco principal promove o proximo; o pedido guarda o texto e nao e afetado
        mockMvc.perform(delete(base + "/{id}", casaDaMae).header("Authorization", maria))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(base).header("Authorization", maria))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].principal").value(true));
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
        var categoria = categoriaRepository.findAll().stream().filter(c -> c.getNome().equals("Elétrica")).findFirst().orElseThrow();

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

    @Test
    void perfilProfissionalCompletoComHorariosFotoEGaleria() throws Exception {
        String joao = token("joao@teste.com", SENHA);
        var categoria = categoriaRepository.findAll().stream().filter(c -> c.getNome().equals("Elétrica")).findFirst().orElseThrow();
        String base = "{\"categoriaId\":" + categoria.getId() + ",\"bairro\":\"Capao Redondo\",\"anosExperiencia\":12,"
                + "\"especialidades\":\"Chuveiros e quadros\",\"diferenciais\":\"Garantia de 90 dias\","
                + "\"diasAtendimento\":[\"SEG\",\"TER\",\"SAB\"],\"atendeEmergencia\":true,";

        mockMvc.perform(put("/api/prestadores/me").header("Authorization", joao).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"horarioInicio\":\"18:00\",\"horarioFim\":\"08:00\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O horário de término deve ser depois do horário de início"));
        mockMvc.perform(put("/api/prestadores/me").header("Authorization", joao).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"horarioInicio\":\"8h\",\"horarioFim\":\"18:00\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/prestadores/me").header("Authorization", joao).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"horarioInicio\":\"08:00\",\"horarioFim\":\"18:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anosExperiencia").value(12))
                .andExpect(jsonPath("$.diasAtendimento[2]").value("SAB"))
                .andExpect(jsonPath("$.atendeEmergencia").value(true));

        String foto = "{\"imagem\":\"data:image/jpeg;base64,/9j/AAAA\"}";
        mockMvc.perform(put("/api/prestadores/me/foto").header("Authorization", joao)
                        .contentType(MediaType.APPLICATION_JSON).content(foto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foto").value("data:image/jpeg;base64,/9j/AAAA"));
        mockMvc.perform(put("/api/prestadores/me/foto").header("Authorization", joao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imagem\":\"http://site/foto.jpg\"}"))
                .andExpect(status().isBadRequest());

        String trabalho = "{\"imagem\":\"data:image/png;base64,iVBORw0KGgo=\",\"legenda\":\"Quadro novo\"}";
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/prestadores/me/fotos").header("Authorization", joao)
                            .contentType(MediaType.APPLICATION_JSON).content(trabalho))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/prestadores/me/fotos").header("Authorization", joao)
                        .contentType(MediaType.APPLICATION_JSON).content(trabalho))
                .andExpect(status().isUnprocessableContent());

        String maria = token("maria@teste.com", SENHA);
        String galeria = mockMvc.perform(get("/api/prestadores/{id}/fotos", prestadorId).header("Authorization", maria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].legenda").value("Quadro novo"))
                .andReturn().getResponse().getContentAsString();
        long fotoId = jsonMapper.readTree(galeria).get(0).get("id").asLong();
        // cliente nao apaga foto de ninguem; o dono apaga a propria
        mockMvc.perform(delete("/api/prestadores/me/fotos/{id}", fotoId).header("Authorization", maria))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/prestadores/me/fotos/{id}", fotoId).header("Authorization", joao))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/prestadores/usuario/{id}", prestadorUsuarioId).header("Authorization", maria))
                .andExpect(jsonPath("$.quantidadeFotos").value(5))
                .andExpect(jsonPath("$.foto").exists())
                .andExpect(jsonPath("$.especialidades").value("Chuveiros e quadros"));
    }
}
