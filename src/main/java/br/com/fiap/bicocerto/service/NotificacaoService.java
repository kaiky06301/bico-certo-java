package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.NotificacaoResponse;
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
            case ASSUMIDA -> avisar(solicitacao, solicitacao.getCliente().getId(),
                    solicitacao.getPrestador().getUsuario().getNome() + " vai atender o seu pedido de "
                            + nomeCategoria(solicitacao) + ". O telefone dele ja esta liberado no pedido.");
            case CONCLUIDA -> avisar(solicitacao, solicitacao.getCliente().getId(),
                    "Servico de " + nomeCategoria(solicitacao) + " concluido! Avalie o prestador para ajudar seus vizinhos.");
            case CANCELADA -> {
                if (solicitacao.getPrestador() != null) {
                    avisar(solicitacao, solicitacao.getPrestador().getUsuario().getId(),
                            "O cliente cancelou o pedido #" + solicitacao.getId() + ".");
                }
            }
        }
    }

    public Page<NotificacaoResponse> listar(Long usuarioId, Pageable pageable) {
        return notificacaoRepository.findByUsuarioId(usuarioId, pageable).map(NotificacaoResponse::from);
    }

    public long naoLidas(Long usuarioId) {
        return notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public NotificacaoResponse marcarComoLida(Long id, Long usuarioId) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificacao " + id + " nao encontrada"));
        if (!notificacao.getUsuario().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Esta notificacao pertence a outro usuario");
        }
        notificacao.marcarComoLida();
        return NotificacaoResponse.from(notificacao);
    }

    private void avisarPrestadores(Solicitacao s) {
        List<Prestador> prestadores = prestadorRepository.findByCategoriaId(s.getCategoria().getId());
        for (Prestador p : prestadores) {
            boolean mesmoBairro = p.getUsuario().getBairro().equalsIgnoreCase(s.getBairro());
            String mensagem = "Novo pedido de " + nomeCategoria(s) + (mesmoBairro ? " no seu bairro (" : " em ")
                    + s.getBairro() + (mesmoBairro ? ")" : "") + " - urgencia " + s.getUrgencia()
                    + ", preco justo R$ " + s.getPrecoMinimo() + " a R$ " + s.getPrecoMaximo() + ".";
            notificacaoRepository.save(new Notificacao(p.getUsuario(), s, mensagem));
        }
        log.info("Solicitacao {}: {} prestador(es) notificado(s)", s.getId(), prestadores.size());
    }

    private void avisar(Solicitacao s, Long usuarioId, String mensagem) {
        var usuario = s.getCliente().getId().equals(usuarioId) ? s.getCliente() : s.getPrestador().getUsuario();
        notificacaoRepository.save(new Notificacao(usuario, s, mensagem));
    }

    private static String nomeCategoria(Solicitacao s) {
        return s.getCategoria().getNome().toLowerCase();
    }
}
