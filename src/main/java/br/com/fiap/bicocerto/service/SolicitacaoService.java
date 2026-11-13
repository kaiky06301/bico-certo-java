package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.SolicitacaoRequest;
import br.com.fiap.bicocerto.dto.SolicitacaoResponse;
import br.com.fiap.bicocerto.dto.TriagemResponse;
import br.com.fiap.bicocerto.exception.AcessoNegadoException;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.messaging.SolicitacaoEvento;
import br.com.fiap.bicocerto.messaging.SolicitacaoEventoProducer;
import br.com.fiap.bicocerto.messaging.TipoEvento;
import br.com.fiap.bicocerto.model.*;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import br.com.fiap.bicocerto.repository.SolicitacaoRepository;
import br.com.fiap.bicocerto.repository.UsuarioRepository;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrestadorRepository prestadorRepository;
    private final TriagemService triagemService;
    private final SolicitacaoEventoProducer eventoProducer;

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository, UsuarioRepository usuarioRepository,
                              PrestadorRepository prestadorRepository, TriagemService triagemService,
                              SolicitacaoEventoProducer eventoProducer) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.prestadorRepository = prestadorRepository;
        this.triagemService = triagemService;
        this.eventoProducer = eventoProducer;
    }

    /** Previa da triagem: o cliente ve categoria, urgencia e preco justo antes de confirmar o pedido. */
    public TriagemResponse simularTriagem(String descricao) {
        ResultadoTriagem resultado = triagemService.triar(descricao.trim());
        long prestadores = prestadorRepository.findByCategoriaId(resultado.categoria().getId()).size();
        return TriagemResponse.from(resultado, prestadores);
    }

    @Transactional
    public SolicitacaoResponse criar(Long clienteId, SolicitacaoRequest request) {
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado"));
        String bairro = request.bairro() == null || request.bairro().isBlank() ? cliente.getBairro() : request.bairro().trim();

        Solicitacao solicitacao = new Solicitacao(cliente, request.descricao().trim(), bairro);
        aplicarTriagem(solicitacao);
        solicitacao = solicitacaoRepository.save(solicitacao);

        eventoProducer.publicarAposCommit(evento(TipoEvento.CRIADA, solicitacao));
        return SolicitacaoResponse.from(solicitacao);
    }

    public Page<SolicitacaoResponse> minhas(Long clienteId, Pageable pageable) {
        return solicitacaoRepository.findByClienteId(clienteId, pageable).map(SolicitacaoResponse::from);
    }

    /** Pedidos ABERTOS da categoria do prestador logado. */
    public Page<SolicitacaoResponse> abertasParaPrestador(Long usuarioId, Pageable pageable) {
        Prestador prestador = prestadorDoUsuario(usuarioId);
        return solicitacaoRepository
                .findByCategoriaIdAndStatus(prestador.getCategoria().getId(), StatusSolicitacao.ABERTA, pageable)
                .map(SolicitacaoResponse::from);
    }

    /** Pedidos que o prestador logado assumiu. */
    public Page<SolicitacaoResponse> meusAtendimentos(Long usuarioId, Pageable pageable) {
        Prestador prestador = prestadorDoUsuario(usuarioId);
        return solicitacaoRepository.findByPrestadorId(prestador.getId(), pageable).map(SolicitacaoResponse::from);
    }

    public Page<SolicitacaoResponse> todas(StatusSolicitacao status, Pageable pageable) {
        Page<Solicitacao> pagina = status == null ? solicitacaoRepository.findAll(pageable)
                : solicitacaoRepository.findByStatus(status, pageable);
        return pagina.map(SolicitacaoResponse::from);
    }

    public SolicitacaoResponse buscar(Long id, UsuarioAutenticado usuario) {
        Solicitacao solicitacao = obter(id);
        verificarLeitura(solicitacao, usuario);
        return SolicitacaoResponse.from(solicitacao);
    }

    /** O cliente pode corrigir a descricao enquanto ninguem assumiu; a triagem e refeita. */
    @Transactional
    public SolicitacaoResponse atualizar(Long id, Long clienteId, SolicitacaoRequest request) {
        Solicitacao solicitacao = obterDoCliente(id, clienteId);
        if (solicitacao.getStatus() != StatusSolicitacao.ABERTA) {
            throw new RegraNegocioException("So e possivel editar pedidos com status ABERTA");
        }
        solicitacao.setDescricao(request.descricao().trim());
        if (request.bairro() != null && !request.bairro().isBlank()) {
            solicitacao.setBairro(request.bairro().trim());
        }
        aplicarTriagem(solicitacao);
        return SolicitacaoResponse.from(solicitacao);
    }

    /** Exclusao logica: o pedido fica no historico como CANCELADA. */
    @Transactional
    public void cancelar(Long id, Long clienteId) {
        Solicitacao solicitacao = obterDoCliente(id, clienteId);
        if (!EnumSet.of(StatusSolicitacao.ABERTA, StatusSolicitacao.EM_ATENDIMENTO).contains(solicitacao.getStatus())) {
            throw new RegraNegocioException("Pedido " + solicitacao.getStatus() + " nao pode ser cancelado");
        }
        solicitacao.setStatus(StatusSolicitacao.CANCELADA);
        eventoProducer.publicarAposCommit(evento(TipoEvento.CANCELADA, solicitacao));
    }

    @Transactional
    public SolicitacaoResponse assumir(Long id, Long usuarioId) {
        Prestador prestador = prestadorDoUsuario(usuarioId);
        Solicitacao solicitacao = solicitacaoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao " + id + " nao encontrada"));
        if (!solicitacao.getCategoria().getId().equals(prestador.getCategoria().getId())) {
            throw new AcessoNegadoException("Este pedido nao e da sua categoria de servico");
        }
        if (solicitacao.getStatus() != StatusSolicitacao.ABERTA) {
            throw new RegraNegocioException("Este pedido ja foi assumido ou nao esta mais aberto");
        }
        solicitacao.setPrestador(prestador);
        solicitacao.setStatus(StatusSolicitacao.EM_ATENDIMENTO);
        eventoProducer.publicarAposCommit(evento(TipoEvento.ASSUMIDA, solicitacao));
        return SolicitacaoResponse.from(solicitacao);
    }

    @Transactional
    public SolicitacaoResponse concluir(Long id, Long usuarioId) {
        Prestador prestador = prestadorDoUsuario(usuarioId);
        Solicitacao solicitacao = solicitacaoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao " + id + " nao encontrada"));
        if (solicitacao.getPrestador() == null || !solicitacao.getPrestador().getId().equals(prestador.getId())) {
            throw new AcessoNegadoException("So o prestador que assumiu o pedido pode concluir");
        }
        if (solicitacao.getStatus() != StatusSolicitacao.EM_ATENDIMENTO) {
            throw new RegraNegocioException("So e possivel concluir pedidos EM_ATENDIMENTO");
        }
        solicitacao.setStatus(StatusSolicitacao.CONCLUIDA);
        eventoProducer.publicarAposCommit(evento(TipoEvento.CONCLUIDA, solicitacao));
        return SolicitacaoResponse.from(solicitacao);
    }

    public Solicitacao obter(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao " + id + " nao encontrada"));
    }

    private Solicitacao obterDoCliente(Long id, Long clienteId) {
        Solicitacao solicitacao = obter(id);
        if (!solicitacao.getCliente().getId().equals(clienteId)) {
            throw new AcessoNegadoException("Este pedido pertence a outro cliente");
        }
        return solicitacao;
    }

    private void verificarLeitura(Solicitacao s, UsuarioAutenticado usuario) {
        switch (usuario.getPapel()) {
            case ADMIN -> {
            }
            case CLIENTE -> {
                if (!s.getCliente().getId().equals(usuario.getId())) {
                    throw new AcessoNegadoException("Este pedido pertence a outro cliente");
                }
            }
            case PRESTADOR -> {
                Prestador prestador = prestadorDoUsuario(usuario.getId());
                boolean ehDele = s.getPrestador() != null && s.getPrestador().getId().equals(prestador.getId());
                boolean abertoNaCategoria = s.getStatus() == StatusSolicitacao.ABERTA
                        && s.getCategoria().getId().equals(prestador.getCategoria().getId());
                if (!ehDele && !abertoNaCategoria) {
                    throw new AcessoNegadoException("Voce nao tem acesso a este pedido");
                }
            }
        }
    }

    private void aplicarTriagem(Solicitacao solicitacao) {
        ResultadoTriagem t = triagemService.triar(solicitacao.getDescricao());
        solicitacao.aplicarTriagem(t.categoria(), t.urgencia(), t.precoMinimo(), t.precoMaximo(), t.resumo(),
                t.dicaSeguranca(), t.origem());
    }

    private Prestador prestadorDoUsuario(Long usuarioId) {
        return prestadorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil de prestador nao encontrado"));
    }

    private static SolicitacaoEvento evento(TipoEvento tipo, Solicitacao s) {
        return new SolicitacaoEvento(tipo, s.getId(), s.getCategoria().getId(), s.getBairro(), s.getUrgencia().name(),
                s.getResumoTriagem(), LocalDateTime.now());
    }
}
