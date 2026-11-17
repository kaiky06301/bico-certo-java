package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.FotoResponse;
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
import br.com.fiap.bicocerto.repository.SolicitacaoFotoRepository;
import br.com.fiap.bicocerto.repository.SolicitacaoRepository;
import br.com.fiap.bicocerto.repository.UsuarioRepository;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrestadorRepository prestadorRepository;
    private final TriagemService triagemService;
    private final SolicitacaoEventoProducer eventoProducer;
    private final EnderecoService enderecoService;
    private final SolicitacaoFotoRepository fotoRepository;

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository, UsuarioRepository usuarioRepository,
                              PrestadorRepository prestadorRepository, TriagemService triagemService,
                              SolicitacaoEventoProducer eventoProducer, EnderecoService enderecoService,
                              SolicitacaoFotoRepository fotoRepository) {
        this.fotoRepository = fotoRepository;
        this.enderecoService = enderecoService;
        this.solicitacaoRepository = solicitacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.prestadorRepository = prestadorRepository;
        this.triagemService = triagemService;
        this.eventoProducer = eventoProducer;
    }

    /** Previa da triagem: o cliente ve categoria, urgencia e preco justo antes de confirmar o pedido. */
    public TriagemResponse simularTriagem(String descricao, String observacao) {
        String obs = vazioParaNulo(observacao);
        String texto = obs == null ? descricao.trim() : descricao.trim() + "\nInformacao adicional: " + obs;
        ResultadoTriagem resultado = triagemService.triar(texto);
        long prestadores = prestadorRepository.findByCategoriaId(resultado.categoria().getId()).size();
        return TriagemResponse.from(resultado, prestadores);
    }

    @Transactional
    public SolicitacaoResponse criar(Long clienteId, SolicitacaoRequest request) {
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado"));
        String bairro = request.bairro() == null || request.bairro().isBlank() ? cliente.getBairro() : request.bairro().trim();

        Solicitacao solicitacao = new Solicitacao(cliente, request.descricao().trim(), bairro);
        solicitacao.setObservacao(vazioParaNulo(request.observacao()));
        aplicarEndereco(solicitacao, clienteId, request.enderecoId());
        aplicarTriagem(solicitacao);
        solicitacao = solicitacaoRepository.save(solicitacao);
        if (request.fotos() != null) {
            salvarFotos(solicitacao, request.fotos());
        }

        eventoProducer.publicarAposCommit(evento(TipoEvento.CRIADA, solicitacao));
        return SolicitacaoResponse.paraDono(solicitacao);
    }

    public Page<SolicitacaoResponse> minhas(Long clienteId, Pageable pageable) {
        return solicitacaoRepository.findByClienteId(clienteId, pageable).map(SolicitacaoResponse::paraDono);
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
        return solicitacao.getCliente().getId().equals(usuario.getId())
                ? SolicitacaoResponse.paraDono(solicitacao) : SolicitacaoResponse.from(solicitacao);
    }

    /** O cliente pode corrigir a descricao enquanto ninguem assumiu; a triagem e refeita. */
    @Transactional
    public SolicitacaoResponse atualizar(Long id, Long clienteId, SolicitacaoRequest request) {
        Solicitacao solicitacao = obterDoCliente(id, clienteId);
        if (solicitacao.getStatus() != StatusSolicitacao.ABERTA) {
            throw new RegraNegocioException("So e possivel editar pedidos com status ABERTA");
        }
        solicitacao.setDescricao(request.descricao().trim());
        solicitacao.setObservacao(vazioParaNulo(request.observacao()));
        if (request.fotos() != null) {
            fotoRepository.apagarDoPedido(solicitacao.getId());
            salvarFotos(solicitacao, request.fotos());
        }
        if (request.bairro() != null && !request.bairro().isBlank()) {
            solicitacao.setBairro(request.bairro().trim());
        }
        aplicarEndereco(solicitacao, clienteId, request.enderecoId());
        aplicarTriagem(solicitacao);
        return SolicitacaoResponse.paraDono(solicitacao);
    }

    /** Fotos do problema; quem pode ver o pedido pode ver as fotos. */
    public List<FotoResponse> fotos(Long id, UsuarioAutenticado usuario) {
        verificarLeitura(obter(id), usuario);
        return fotoRepository.findBySolicitacaoIdOrderByOrdemAsc(id).stream().map(FotoResponse::from).toList();
    }

    private void salvarFotos(Solicitacao solicitacao, List<String> fotos) {
        for (int i = 0; i < fotos.size(); i++) {
            fotoRepository.save(new SolicitacaoFoto(solicitacao, i + 1, fotos.get(i).replaceAll("\\s", "")));
        }
        solicitacao.setQuantidadeFotos(fotos.size());
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Endereco escolhido em "Meus enderecos": define bairro e rua (o pedido guarda o texto, nao o id). */
    private void aplicarEndereco(Solicitacao solicitacao, Long clienteId, Long enderecoId) {
        if (enderecoId == null) {
            return;
        }
        Endereco endereco = enderecoService.obter(clienteId, enderecoId);
        solicitacao.setBairro(endereco.getBairro());
        solicitacao.setEndereco(endereco.textoCompleto());
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

    /**
     * O cliente aceitou o orcamento no modulo .NET: o pedido passa para o prestador contratado (EM_ATENDIMENTO).
     * A partir daqui o prestador ve o endereco completo e o telefone para ir ate o local.
     */
    @Transactional
    public SolicitacaoResponse registrarContratacao(Long id, Long clienteId, Long prestadorUsuarioId) {
        Solicitacao solicitacao = solicitacaoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao " + id + " nao encontrada"));
        if (!solicitacao.getCliente().getId().equals(clienteId)) {
            throw new AcessoNegadoException("So o cliente dono do pedido pode registrar a contratacao");
        }
        Prestador prestador = prestadorDoUsuario(prestadorUsuarioId);
        if (solicitacao.getStatus() == StatusSolicitacao.EM_ATENDIMENTO
                && solicitacao.getPrestador() != null && solicitacao.getPrestador().getId().equals(prestador.getId())) {
            return SolicitacaoResponse.paraDono(solicitacao); // repeticao (internet instavel): nada muda
        }
        if (!solicitacao.getCategoria().getId().equals(prestador.getCategoria().getId())) {
            throw new RegraNegocioException("O prestador contratado nao atende a categoria deste pedido");
        }
        if (solicitacao.getStatus() != StatusSolicitacao.ABERTA) {
            throw new RegraNegocioException("Este pedido ja foi assumido ou nao esta mais aberto");
        }
        solicitacao.setPrestador(prestador);
        solicitacao.setStatus(StatusSolicitacao.EM_ATENDIMENTO);
        eventoProducer.publicarAposCommit(evento(TipoEvento.ASSUMIDA, solicitacao));
        return SolicitacaoResponse.paraDono(solicitacao);
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
        ResultadoTriagem t = triagemService.triar(solicitacao.textoParaTriagem());
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
