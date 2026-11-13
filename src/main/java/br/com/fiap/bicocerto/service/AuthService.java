package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.LoginRequest;
import br.com.fiap.bicocerto.dto.LoginResponse;
import br.com.fiap.bicocerto.dto.RegistroRequest;
import br.com.fiap.bicocerto.dto.UsuarioResponse;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.model.Categoria;
import br.com.fiap.bicocerto.model.Papel;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.repository.CategoriaRepository;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import br.com.fiap.bicocerto.repository.UsuarioRepository;
import br.com.fiap.bicocerto.security.JwtUtil;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrestadorRepository prestadorRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                       UsuarioRepository usuarioRepository, CategoriaRepository categoriaRepository,
                       PrestadorRepository prestadorRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.prestadorRepository = prestadorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Autentica e devolve o token JWT. Credenciais erradas viram 401 no AuthController. */
    public LoginResponse login(LoginRequest request) {
        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));
        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        Usuario entidade = usuarioRepository.findById(usuario.getId()).orElseThrow();
        return new LoginResponse(jwtUtil.gerarToken(usuario), "Bearer", jwtUtil.getExpiracaoMs(),
                UsuarioResponse.from(entidade));
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (request.papel() == Papel.ADMIN) {
            throw new RegraNegocioException("Nao e permitido se cadastrar como ADMIN");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new RegraNegocioException("Ja existe uma conta com este e-mail");
        }

        Categoria categoria = null;
        if (request.papel() == Papel.PRESTADOR) {
            if (request.categoriaId() == null) {
                throw new RegraNegocioException("Prestador precisa informar a categoria de servico (categoriaId)");
            }
            categoria = categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria nao encontrada"));
        }

        Usuario usuario = usuarioRepository.save(new Usuario(request.nome().trim(), request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.senha()), request.telefone(), request.bairro().trim(), request.papel()));

        if (categoria != null) {
            prestadorRepository.save(new Prestador(usuario, categoria, request.descricaoServicos()));
        }
        return UsuarioResponse.from(usuario);
    }

    public UsuarioResponse perfil(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }
}
