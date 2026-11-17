package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.EnderecoRequest;
import br.com.fiap.bicocerto.dto.EnderecoResponse;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.model.Endereco;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.repository.EnderecoRepository;
import br.com.fiap.bicocerto.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * "Meus enderecos": o usuario guarda varios lugares (casa, casa da mae, trabalho) e escolhe um ao criar o pedido.
 * Regras: no maximo 10; o primeiro vira principal; sempre existe exatamente um principal quando ha enderecos.
 */
@Service
public class EnderecoService {

    static final int LIMITE = 10;

    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;

    public EnderecoService(EnderecoRepository enderecoRepository, UsuarioRepository usuarioRepository) {
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<EnderecoResponse> listar(Long usuarioId) {
        return doUsuario(usuarioId).stream().map(EnderecoResponse::from).toList();
    }

    @Transactional
    public EnderecoResponse criar(Long usuarioId, EnderecoRequest request) {
        if (enderecoRepository.countByUsuarioId(usuarioId) >= LIMITE) {
            throw new RegraNegocioException("Voce pode salvar no maximo " + LIMITE + " enderecos");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
        Endereco endereco = new Endereco(usuario, request.apelido().trim(), request.logradouro().trim(),
                vazioParaNulo(request.complemento()), request.bairro().trim(), request.cidade().trim(),
                request.uf().trim().toUpperCase());
        boolean primeiro = enderecoRepository.countByUsuarioId(usuarioId) == 0;
        endereco = enderecoRepository.save(endereco);
        if (primeiro || Boolean.TRUE.equals(request.principal())) {
            tornarPrincipal(usuarioId, endereco);
        }
        return EnderecoResponse.from(endereco);
    }

    @Transactional
    public EnderecoResponse atualizar(Long usuarioId, Long id, EnderecoRequest request) {
        Endereco endereco = obter(usuarioId, id);
        endereco.atualizar(request.apelido().trim(), request.logradouro().trim(), vazioParaNulo(request.complemento()),
                request.bairro().trim(), request.cidade().trim(), request.uf().trim().toUpperCase());
        if (Boolean.TRUE.equals(request.principal())) {
            tornarPrincipal(usuarioId, endereco);
        }
        return EnderecoResponse.from(endereco);
    }

    @Transactional
    public EnderecoResponse definirPrincipal(Long usuarioId, Long id) {
        Endereco endereco = obter(usuarioId, id);
        tornarPrincipal(usuarioId, endereco);
        return EnderecoResponse.from(endereco);
    }

    /** Ao excluir o principal, o proximo da lista vira principal. Pedidos antigos guardam o texto, nao o id. */
    @Transactional
    public void excluir(Long usuarioId, Long id) {
        Endereco endereco = obter(usuarioId, id);
        boolean eraPrincipal = endereco.isPrincipal();
        enderecoRepository.delete(endereco);
        enderecoRepository.flush();
        if (eraPrincipal) {
            doUsuario(usuarioId).stream().findFirst().ifPresent(e -> e.setPrincipal(true));
        }
    }

    /** Usado ao criar o pedido: o endereco precisa ser do proprio cliente. */
    public Endereco obter(Long usuarioId, Long id) {
        return enderecoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereco " + id + " nao encontrado"));
    }

    private void tornarPrincipal(Long usuarioId, Endereco escolhido) {
        doUsuario(usuarioId).forEach(e -> e.setPrincipal(e.getId().equals(escolhido.getId())));
        escolhido.setPrincipal(true);
    }

    private List<Endereco> doUsuario(Long usuarioId) {
        return enderecoRepository.findByUsuarioIdOrderByPrincipalDescApelidoAsc(usuarioId);
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
