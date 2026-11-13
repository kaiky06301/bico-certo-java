package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.PrestadorPerfilRequest;
import br.com.fiap.bicocerto.dto.PrestadorResponse;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrestadorService {

    private final PrestadorRepository prestadorRepository;
    private final CategoriaService categoriaService;

    public PrestadorService(PrestadorRepository prestadorRepository, CategoriaService categoriaService) {
        this.prestadorRepository = prestadorRepository;
        this.categoriaService = categoriaService;
    }

    public Page<PrestadorResponse> buscar(Long categoriaId, String bairro, Pageable pageable) {
        String filtroBairro = bairro == null || bairro.isBlank() ? null : bairro.trim();
        return prestadorRepository.buscar(categoriaId, filtroBairro, pageable).map(PrestadorResponse::from);
    }

    public PrestadorResponse detalhar(Long id) {
        return PrestadorResponse.from(obter(id));
    }

    public PrestadorResponse meuPerfil(Long usuarioId) {
        return PrestadorResponse.from(doUsuario(usuarioId));
    }

    @Transactional
    public PrestadorResponse atualizarMeuPerfil(Long usuarioId, PrestadorPerfilRequest request) {
        Prestador prestador = doUsuario(usuarioId);
        prestador.setCategoria(categoriaService.obter(request.categoriaId()));
        prestador.setDescricao(request.descricao());
        prestador.getUsuario().setBairro(request.bairro().trim());
        if (request.telefone() != null) {
            prestador.getUsuario().setTelefone(request.telefone());
        }
        return PrestadorResponse.from(prestador);
    }

    public Prestador obter(Long id) {
        return prestadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prestador " + id + " nao encontrado"));
    }

    private Prestador doUsuario(Long usuarioId) {
        return prestadorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil de prestador nao encontrado"));
    }
}
