package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.AvaliacaoRequest;
import br.com.fiap.bicocerto.dto.AvaliacaoResponse;
import br.com.fiap.bicocerto.exception.AcessoNegadoException;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.model.Avaliacao;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.Solicitacao;
import br.com.fiap.bicocerto.model.StatusSolicitacao;
import br.com.fiap.bicocerto.repository.AvaliacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final SolicitacaoService solicitacaoService;
    private final PrestadorService prestadorService;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, SolicitacaoService solicitacaoService,
                            PrestadorService prestadorService) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.solicitacaoService = solicitacaoService;
        this.prestadorService = prestadorService;
    }

    @Transactional
    public AvaliacaoResponse avaliar(Long solicitacaoId, Long clienteId, AvaliacaoRequest request) {
        Solicitacao solicitacao = solicitacaoService.obter(solicitacaoId);
        if (!solicitacao.getCliente().getId().equals(clienteId)) {
            throw new AcessoNegadoException("So o cliente do pedido pode avaliar");
        }
        if (solicitacao.getStatus() != StatusSolicitacao.CONCLUIDA) {
            throw new RegraNegocioException("So e possivel avaliar pedidos CONCLUIDOS");
        }
        if (avaliacaoRepository.existsBySolicitacaoId(solicitacaoId)) {
            throw new RegraNegocioException("Este pedido ja foi avaliado");
        }
        Prestador prestador = solicitacao.getPrestador();
        prestador.registrarAvaliacao(request.nota());
        Avaliacao avaliacao = avaliacaoRepository.save(new Avaliacao(solicitacao, prestador, request.nota(),
                request.comentario()));
        return AvaliacaoResponse.from(avaliacao);
    }

    public Page<AvaliacaoResponse> doPrestador(Long prestadorId, Pageable pageable) {
        prestadorService.obter(prestadorId);
        return avaliacaoRepository.findByPrestadorId(prestadorId, pageable).map(AvaliacaoResponse::from);
    }
}
