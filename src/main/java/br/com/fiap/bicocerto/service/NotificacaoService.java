package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.AvisoRequest;
import br.com.fiap.bicocerto.dto.NotificacaoResponse;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.model.TipoAviso;
import br.com.fiap.bicocerto.model.Usuario;
import br.com.fiap.bicocerto.exception.AcessoNegadoException;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.messaging.SolicitacaoEvento;
import br.com.fiap.bicocerto.model.Notificacao;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.Solicitacao;
import br.com.fiap.bicocerto.repository.NotificacaoRepository;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import br.com.fiap.bicocerto.repository.SolicitacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoRepository notificacaoRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final PrestadorRepository prestadorRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository, SolicitacaoRepository solicitacaoRepository,
                              PrestadorRepository prestadorRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.prestadorRepository = prestadorRepository;
    }

    /** Chamado pelo consumidor do RabbitMQ para cada evento recebido. */
    @Transactional
    public void processar(SolicitacaoEvento evento) {
        Solicitacao solicitacao = solicitacaoRepository.findById(evento.solicitacaoId()).orElse(null);
        if (solicitacao == null) {
            log.warn("Evento {} ignorado: solicitacao {} nao existe", evento.tipo(), evento.solicitacaoId());
            return;
        }
        switch (evento.tipo()) {
            case CRIADA -> avisarPrestadores(solicitacao);
            case ASSUMIDA -> avisar(solicitacao, solicitacao.getCliente().getId(), TipoAviso.CONTRATACAO,
                    solicitacao.getPrestador().getUsuario().getNome() + " vai atender o seu pedido de "
                            + nomeCategoria(solicitacao) + ". O telefone e o endereço já estão liberados no pedido.");
            case CONCLUIDA -> avisar(solicitacao, solicitacao.getCliente().getId(), TipoAviso.CONCLUSAO,
                    "Serviço de " + nomeCategoria(solicitacao) + " concluído! Avalie o prestador para ajudar seus vizinhos.");
            case CANCELADA -> {
                if (solicitacao.getPrestador() != null) {
                    avisar(solicitacao, solicitacao.getPrestador().getUsuario().getId(), TipoAviso.CANCELAMENTO,
                            "O cliente cancelou o pedido #" + solicitacao.getId()
                                    + (solicitacao.getMotivoCancelamento() == null ? "." : ". Motivo: "
                                    + solicitacao.getMotivoCancelamento()));
                }
            }
        }
    }

    public Page<NotificacaoResponse> listar(Long usuarioId, Boolean lida, Pageable pageable) {
        Page<Notificacao> pagina = lida == null ? notificacaoRepository.findByUsuarioId(usuarioId, pageable)
                : notificacaoRepository.findByUsuarioIdAndLida(usuarioId, lida, pageable);
        return pagina.map(NotificacaoResponse::from);
    }

    @Transactional
    public int marcarTodasComoLidas(Long usuarioId) {
        return notificacaoRepository.marcarTodasComoLidas(usuarioId);
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        notificacaoRepository.delete(daPessoa(id, usuarioId));
    }

    /** "Limpar avisos lidos": os nao lidos ficam. */
    @Transactional
    public int limparLidas(Long usuarioId) {
        return notificacaoRepository.apagarLidas(usuarioId);
    }

    /**
     * Aviso vindo do modulo .NET. Quem envia e quem recebe precisam participar do pedido:
     * o cliente dono ou um prestador da categoria (se o pedido ja tem prestador, so ele).
     */
    @Transactional
    public NotificacaoResponse registrarAviso(Long solicitacaoId, Long remetenteId, AvisoRequest req) {
        Solicitacao s = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao " + solicitacaoId + " nao encontrada"));
        if (remetenteId.equals(req.destinatarioId())) {
            throw new RegraNegocioException("O aviso precisa ser para a outra parte do pedido");
        }
        if (!participa(s, remetenteId)) {
            throw new AcessoNegadoException("Voce nao participa deste pedido");
        }
        Usuario destinatario = s.getCliente().getId().equals(req.destinatarioId()) ? s.getCliente()
                : prestadorRepository.findByUsuarioId(req.destinatarioId())
                        .filter(p -> participa(s, p.getUsuario().getId()))
                        .map(Prestador::getUsuario)
                        .orElseThrow(() -> new RegraNegocioException("O destinatario nao participa deste pedido"));
        Notificacao n = notificacaoRepository.save(new Notificacao(destinatario, s, req.tipo(), req.mensagem().trim(),
                req.valor(), req.orcamentoId()));
        return NotificacaoResponse.from(n);
    }

    private boolean participa(Solicitacao s, Long usuarioId) {
        if (s.getCliente().getId().equals(usuarioId)) {
            return true;
        }
        return prestadorRepository.findByUsuarioId(usuarioId)
                .map(p -> s.getPrestador() != null ? s.getPrestador().getId().equals(p.getId())
                        : p.getCategoria().getId().equals(s.getCategoria().getId()))
                .orElse(false);
    }

    private Notificacao daPessoa(Long id, Long usuarioId) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificacao " + id + " nao encontrada"));
        if (!notificacao.getUsuario().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Esta notificacao pertence a outro usuario");
        }
        return notificacao;
    }

    public long naoLidas(Long usuarioId) {
        return notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public NotificacaoResponse marcarComoLida(Long id, Long usuarioId) {
        Notificacao notificacao = daPessoa(id, usuarioId);
        notificacao.marcarComoLida();
        return NotificacaoResponse.from(notificacao);
    }

    private void avisarPrestadores(Solicitacao s) {
        List<Prestador> prestadores = prestadorRepository.findByCategoriaId(s.getCategoria().getId());
        for (Prestador p : prestadores) {
            boolean mesmoBairro = p.getUsuario().getBairro().equalsIgnoreCase(s.getBairro());
            String mensagem = "Novo pedido de " + nomeCategoria(s) + (mesmoBairro ? " no seu bairro (" : " em ")
                    + s.getBairro() + (mesmoBairro ? ")" : "") + ", urgência " + s.getUrgencia().name().toLowerCase()
                    + ". Faixa de referência: R$ " + s.getPrecoMinimo() + " a R$ " + s.getPrecoMaximo() + ".";
            notificacaoRepository.save(new Notificacao(p.getUsuario(), s, mensagem));
        }
        log.info("Solicitacao {}: {} prestador(es) notificado(s)", s.getId(), prestadores.size());
    }

    private void avisar(Solicitacao s, Long usuarioId, TipoAviso tipo, String mensagem) {
        var usuario = s.getCliente().getId().equals(usuarioId) ? s.getCliente() : s.getPrestador().getUsuario();
        notificacaoRepository.save(new Notificacao(usuario, s, tipo, mensagem, null, null));
    }

    private static String nomeCategoria(Solicitacao s) {
        return s.getCategoria().getNome().toLowerCase();
    }
}
