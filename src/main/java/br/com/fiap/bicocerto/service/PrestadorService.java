package br.com.fiap.bicocerto.service;

import br.com.fiap.bicocerto.dto.PrestadorPerfilRequest;
import br.com.fiap.bicocerto.dto.PrestadorResponse;
import br.com.fiap.bicocerto.dto.TrabalhoFotoRequest;
import br.com.fiap.bicocerto.dto.TrabalhoFotoResponse;
import br.com.fiap.bicocerto.exception.RecursoNaoEncontradoException;
import br.com.fiap.bicocerto.exception.RegraNegocioException;
import br.com.fiap.bicocerto.model.Prestador;
import br.com.fiap.bicocerto.model.PrestadorFoto;
import br.com.fiap.bicocerto.repository.PrestadorFotoRepository;
import br.com.fiap.bicocerto.repository.PrestadorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrestadorService {

    private final PrestadorRepository prestadorRepository;
    private final CategoriaService categoriaService;
    private final PrestadorFotoRepository fotoRepository;

    /** Limite da galeria de trabalhos realizados. */
    public static final int MAXIMO_FOTOS_GALERIA = 6;

    public PrestadorService(PrestadorRepository prestadorRepository, CategoriaService categoriaService,
                            PrestadorFotoRepository fotoRepository) {
        this.prestadorRepository = prestadorRepository;
        this.categoriaService = categoriaService;
        this.fotoRepository = fotoRepository;
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
        if (request.horarioInicio() != null && request.horarioFim() != null
                && request.horarioInicio().compareTo(request.horarioFim()) >= 0) {
            throw new RegraNegocioException("O horário de término deve ser depois do horário de início");
        }
        prestador.setCategoria(categoriaService.obter(request.categoriaId()));
        List<Long> outras = request.outrasCategoriaIds() == null ? List.of() : request.outrasCategoriaIds();
        prestador.definirOutrasCategorias(outras.stream().distinct().map(categoriaService::obter).toList());
        prestador.setDescricao(vazioParaNulo(request.descricao()));
        prestador.setRegiaoAtendimento(vazioParaNulo(request.regiaoAtendimento()));
        prestador.setDisponibilidade(vazioParaNulo(request.disponibilidade()));
        prestador.definirExperiencia(request.anosExperiencia(), vazioParaNulo(request.especialidades()),
                vazioParaNulo(request.diferenciais()));
        prestador.definirAtendimento(request.diasAtendimento() == null ? List.of() : request.diasAtendimento().stream().distinct().toList(),
                request.horarioInicio(), request.horarioFim(), Boolean.TRUE.equals(request.atendeEmergencia()));
        prestador.getUsuario().setBairro(request.bairro().trim());
        if (request.telefone() != null) {
            prestador.getUsuario().setTelefone(request.telefone());
        }
        return PrestadorResponse.from(prestador);
    }

    @Transactional
    public PrestadorResponse definirFoto(Long usuarioId, String imagem) {
        Prestador prestador = doUsuario(usuarioId);
        prestador.setFoto(imagem);
        return PrestadorResponse.from(prestador);
    }

    /** Galeria de trabalhos realizados, mais recentes primeiro (publica, como o resto do perfil). */
    public List<TrabalhoFotoResponse> galeria(Long prestadorId) {
        obter(prestadorId);
        return fotoRepository.findByPrestadorIdOrderByCriadaEmDesc(prestadorId).stream()
                .map(TrabalhoFotoResponse::from).toList();
    }

    @Transactional
    public TrabalhoFotoResponse adicionarFotoNaGaleria(Long usuarioId, TrabalhoFotoRequest request) {
        Prestador prestador = doUsuario(usuarioId);
        if (fotoRepository.countByPrestadorId(prestador.getId()) >= MAXIMO_FOTOS_GALERIA) {
            throw new RegraNegocioException("A galeria aceita até " + MAXIMO_FOTOS_GALERIA + " fotos. Apague uma para enviar outra");
        }
        PrestadorFoto foto = new PrestadorFoto(prestador, request.imagem(), vazioParaNulo(request.legenda()));
        return TrabalhoFotoResponse.from(fotoRepository.save(foto));
    }

    @Transactional
    public void removerFotoDaGaleria(Long usuarioId, Long fotoId) {
        Prestador prestador = doUsuario(usuarioId);
        PrestadorFoto foto = fotoRepository.findByIdAndPrestadorId(fotoId, prestador.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Foto " + fotoId + " não encontrada na sua galeria"));
        fotoRepository.delete(foto);
    }

    /** O modulo .NET guarda o id do usuario (claim uid): o app usa esta busca para abrir o perfil publico. */
    public PrestadorResponse doUsuarioPublico(Long usuarioId) {
        return PrestadorResponse.from(doUsuario(usuarioId));
    }

    @Transactional
    public PrestadorResponse definirVerificacao(Long id, boolean verificado) {
        Prestador prestador = obter(id);
        prestador.setVerificado(verificado);
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

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
