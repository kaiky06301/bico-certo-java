package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.CategoriaRequest;
import br.com.fiap.bicocerto.dto.CategoriaResponse;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.model.Categoria;
import br.com.fiap.bicocerto.repository.CategoriaRepository;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import br.com.fiap.bicocerto.repository.SolicitacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final PrestadorRepository prestadorRepository;
    private final SolicitacaoRepository solicitacaoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, PrestadorRepository prestadorRepository,
                            SolicitacaoRepository solicitacaoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.prestadorRepository = prestadorRepository;
        this.solicitacaoRepository = solicitacaoRepository;
    }

    public Page<CategoriaResponse> listar(Pageable pageable) {
        return categoriaRepository.findAll(pageable).map(CategoriaResponse::from);
    }

    public List<Categoria> todas() {
        return categoriaRepository.findAll();
    }

    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.from(obter(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        validarFaixa(request);
        if (categoriaRepository.existsByNomeIgnoreCase(request.nome().trim())) {
            throw new RegraNegocioException("Ja existe uma categoria com este nome");
        }
        Categoria categoria = new Categoria(request.nome().trim(), request.descricao().trim(),
                request.precoMinimo(), request.precoMaximo());
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        validarFaixa(request);
        Categoria categoria = obter(id);
        categoria.atualizar(request.nome().trim(), request.descricao().trim(), request.precoMinimo(), request.precoMaximo());
        return CategoriaResponse.from(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = obter(id);
        if (prestadorRepository.existsByCategoriaId(id) || prestadorRepository.existsByOutrasCategoriasId(id) || solicitacaoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Categoria em uso por prestadores ou solicitacoes nao pode ser excluida");
        }
        categoriaRepository.delete(categoria);
    }

    public Categoria obter(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " nao encontrada"));
    }

    private void validarFaixa(CategoriaRequest request) {
        if (request.precoMinimo().compareTo(request.precoMaximo()) > 0) {
            throw new RegraNegocioException("Preco minimo nao pode ser maior que o preco maximo");
        }
    }
}
