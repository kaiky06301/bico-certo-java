package br.com.fiap.bicocerto.config;

import br.com.fiap.bicocerto.model.Categoria;
import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.repository.CategoriaRepository;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import br.com.fiap.bicocerto.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cria os usuarios de teste na primeira execucao (a senha vem da variavel SEED_PASSWORD).
 * As categorias ja sao criadas pela migration V2 do Flyway.
 */
@Component
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrestadorRepository prestadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final String senhaPadrao;

    public DataLoader(UsuarioRepository usuarioRepository, CategoriaRepository categoriaRepository,
                      PrestadorRepository prestadorRepository, PasswordEncoder passwordEncoder,
                      @Value("${SEED_PASSWORD:}") String senhaPadrao) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.prestadorRepository = prestadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaPadrao = senhaPadrao;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (senhaPadrao.isBlank()) {
            log.warn("SEED_PASSWORD nao definida: usuarios de teste nao foram criados");
            return;
        }
        String senha = passwordEncoder.encode(senhaPadrao);

        usuarioRepository.save(new Usuario("Administrador Bico Certo", "admin@bicocerto.com.br", senha,
                "(11) 90000-0000", "Centro", Papel.ADMIN));
        usuarioRepository.save(new Usuario("Maria Souza", "maria@bicocerto.com.br", senha,
                "(11) 91111-1111", "Capão Redondo", Papel.CLIENTE));
        usuarioRepository.save(new Usuario("Pedro Lima", "pedro@bicocerto.com.br", senha,
                "(11) 92222-2222", "Jardim Ângela", Papel.CLIENTE));

        List<Categoria> categorias = categoriaRepository.findAll();
        criarPrestador("João Eletricista", "joao@bicocerto.com.br", senha, "Capão Redondo",
                categoria(categorias, "Elétrica"), "Eletricista há 12 anos, atendo emergências no mesmo dia.",
                "Capão Redondo e bairros próximos", "Segunda a sábado, das 8h às 18h", true);
        criarPrestador("Roberto Alves", "roberto@bicocerto.com.br", senha, "Capão Redondo",
                categoria(categorias, "Elétrica"), "Eletricista residencial, trabalho com garantia de 90 dias.",
                "Capão Redondo e Campo Limpo", "Segunda a sexta, das 9h às 18h", false);
        criarPrestador("Carlos Encanador", "carlos@bicocerto.com.br", senha, "Capão Redondo",
                categoria(categorias, "Encanamento"), "Conserto vazamentos, desentupo pias e troco registros.",
                "Capão Redondo e bairros próximos", "Segunda a sábado, das 8h às 18h", true);
        criarPrestador("Ana Diarista", "ana@bicocerto.com.br", senha, "Jardim Ângela",
                categoria(categorias, "Limpeza"), "Faxina completa e pós-obra com material próprio.",
                "Jardim Ângela e Capão Redondo", "Segunda a sexta, das 7h às 17h", false);

        log.info("Usuarios de teste criados (admin, maria, pedro, joao, roberto, carlos, ana)");
    }

    private void criarPrestador(String nome, String email, String senha, String bairro, Categoria categoria,
                                String descricao, String regiao, String disponibilidade, boolean verificado) {
        Usuario usuario = usuarioRepository.save(new Usuario(nome, email, senha, "(11) 93333-3333", bairro,
                Papel.PRESTADOR));
        Prestador prestador = new Prestador(usuario, categoria, descricao);
        prestador.setRegiaoAtendimento(regiao);
        prestador.setDisponibilidade(disponibilidade);
        prestador.setVerificado(verificado);
        prestadorRepository.save(prestador);
    }

    private Categoria categoria(List<Categoria> categorias, String nome) {
        return categorias.stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nome))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Categoria " + nome + " nao encontrada na migration"));
    }
}
