package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.AlterarSenhaRequest;
import br.com.fiap.bicocerto.dto.AvaliacaoResponse;
import br.com.fiap.bicocerto.dto.ContaRequest;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.dto.UsuarioResponse;
import br.com.fiap.bicocerto.model.StatusSolicitacao;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.repository.AvaliacaoRepository;
import br.com.fiap.bicocerto.repository.EnderecoRepository;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import br.com.fiap.bicocerto.repository.SolicitacaoRepository;
import br.com.fiap.bicocerto.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** "Minha conta": dados pessoais, troca de senha, avaliacoes feitas e exclusao da conta. */
@Service
public class ContaService {

    private final UsuarioRepository usuarioRepository;
    private final PrestadorRepository prestadorRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final EnderecoRepository enderecoRepository;
    private final PasswordEncoder passwordEncoder;

    public ContaService(UsuarioRepository usuarioRepository, PrestadorRepository prestadorRepository,
                        SolicitacaoRepository solicitacaoRepository, AvaliacaoRepository avaliacaoRepository,
                        EnderecoRepository enderecoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.prestadorRepository = prestadorRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.enderecoRepository = enderecoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponse minhaConta(Long usuarioId) {
        return UsuarioResponse.from(obter(usuarioId));
    }

    @Transactional
    public UsuarioResponse atualizar(Long usuarioId, ContaRequest request) {
        Usuario usuario = obter(usuarioId);
        usuario.setNome(request.nome().trim());
        usuario.setTelefone(vazioParaNulo(request.telefone()));
        usuario.setBairro(request.bairro().trim());
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public void alterarSenha(Long usuarioId, AlterarSenhaRequest request) {
        Usuario usuario = obter(usuarioId);
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new RegraNegocioException("A senha atual nao confere");
        }
        if (request.senhaAtual().equals(request.novaSenha())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual");
        }
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
    }

    /** Avaliacoes que o cliente fez dos prestadores. */
    public Page<AvaliacaoResponse> avaliacoesFeitas(Long usuarioId, Pageable pageable) {
        return avaliacaoRepository.findBySolicitacaoClienteId(usuarioId, pageable).map(AvaliacaoResponse::from);
    }

    /**
     * Exclui a conta: exige a senha e bloqueia se houver servico em andamento (para o outro lado nao ficar sem
     * contato). Os dados pessoais (inclusive enderecos salvos e a rua dos pedidos) sao apagados e o login para de funcionar.
     */
    @Transactional
    public void excluir(Long usuarioId, String senha) {
        Usuario usuario = obter(usuarioId);
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new RegraNegocioException("Senha incorreta");
        }
        boolean pedidoAtivo = solicitacaoRepository.existsByClienteIdAndStatusIn(usuarioId,
                List.of(StatusSolicitacao.ABERTA, StatusSolicitacao.EM_ATENDIMENTO));
        boolean atendimentoAtivo = prestadorRepository.findByUsuarioId(usuarioId)
                .map(p -> solicitacaoRepository.existsByPrestadorIdAndStatus(p.getId(), StatusSolicitacao.EM_ATENDIMENTO))
                .orElse(false);
        if (pedidoAtivo || atendimentoAtivo) {
            throw new RegraNegocioException("Voce tem pedidos ou servicos em andamento. Cancele ou conclua antes de excluir a conta");
        }
        enderecoRepository.deleteByUsuarioId(usuarioId);
        solicitacaoRepository.limparEnderecosDoCliente(usuarioId);
        usuario.anonimizar(passwordEncoder.encode(UUID.randomUUID().toString()));
    }

    private Usuario obter(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
