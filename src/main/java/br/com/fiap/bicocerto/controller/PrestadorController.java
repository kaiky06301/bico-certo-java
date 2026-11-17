package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.dto.AvaliacaoResponse;
import br.com.fiap.bicocerto.dto.PaginaResponse;
import br.com.fiap.bicocerto.dto.PrestadorPerfilRequest;
import br.com.fiap.bicocerto.dto.PrestadorResponse;
import br.com.fiap.bicocerto.dto.TrabalhoFotoRequest;
import br.com.fiap.bicocerto.dto.TrabalhoFotoResponse;
import br.com.fiap.bicocerto.dto.VerificacaoRequest;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import br.com.fiap.bicocerto.service.AvaliacaoService;
import br.com.fiap.bicocerto.service.PrestadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestadores")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Prestadores", description = "Busca de prestadores do bairro, reputacao e perfil")
public class PrestadorController {

    private final PrestadorService prestadorService;
    private final AvaliacaoService avaliacaoService;

    public PrestadorController(PrestadorService prestadorService, AvaliacaoService avaliacaoService) {
        this.prestadorService = prestadorService;
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping
    @Operation(summary = "Busca prestadores por categoria e/ou bairro, ordenados pela melhor nota")
    public PaginaResponse<PrestadorResponse> buscar(@RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String bairro,
            @ParameterObject @PageableDefault(size = 10, sort = "notaMedia", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(prestadorService.buscar(categoriaId, bairro, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um prestador")
    public PrestadorResponse detalhar(@PathVariable Long id) {
        return prestadorService.detalhar(id);
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Perfil publico do prestador a partir do id do usuario (o modulo .NET guarda esse id)")
    public PrestadorResponse porUsuario(@PathVariable Long usuarioId) {
        return prestadorService.doUsuarioPublico(usuarioId);
    }

    @PatchMapping("/{id}/verificacao")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Concede ou retira o selo 'Verificado pelo Bico Certo' (ADMIN, depois de conferir documentos)")
    public PrestadorResponse verificar(@PathVariable Long id, @RequestBody @Valid VerificacaoRequest request) {
        return prestadorService.definirVerificacao(id, request.verificado());
    }

    @GetMapping("/{id}/avaliacoes")
    @Operation(summary = "Avaliacoes recebidas pelo prestador")
    public PaginaResponse<AvaliacaoResponse> avaliacoes(@PathVariable Long id,
            @ParameterObject @PageableDefault(size = 10, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(avaliacaoService.doPrestador(id, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Perfil do prestador logado")
    public PrestadorResponse meuPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return prestadorService.meuPerfil(usuario.getId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Atualiza o perfil profissional do prestador logado (servicos, experiencia, regiao e horarios)")
    public PrestadorResponse atualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @RequestBody @Valid PrestadorPerfilRequest request) {
        return prestadorService.atualizarMeuPerfil(usuario.getId(), request);
    }

    @PutMapping("/me/foto")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Define a foto de perfil do prestador logado (data URI JPEG/PNG)")
    public PrestadorResponse definirFoto(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @RequestBody @Valid TrabalhoFotoRequest request) {
        return prestadorService.definirFoto(usuario.getId(), request.imagem());
    }

    @DeleteMapping("/me/foto")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Remove a foto de perfil do prestador logado")
    public PrestadorResponse removerFoto(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return prestadorService.definirFoto(usuario.getId(), null);
    }

    @GetMapping("/{id}/fotos")
    @Operation(summary = "Galeria de trabalhos realizados pelo prestador")
    public List<TrabalhoFotoResponse> galeria(@PathVariable Long id) {
        return prestadorService.galeria(id);
    }

    @PostMapping("/me/fotos")
    @PreAuthorize("hasRole('PRESTADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona uma foto de trabalho realizado na galeria (ate 6)")
    public TrabalhoFotoResponse adicionarFoto(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @RequestBody @Valid TrabalhoFotoRequest request) {
        return prestadorService.adicionarFotoNaGaleria(usuario.getId(), request);
    }

    @DeleteMapping("/me/fotos/{fotoId}")
    @PreAuthorize("hasRole('PRESTADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma foto da galeria do prestador logado")
    public void removerFotoDaGaleria(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long fotoId) {
        prestadorService.removerFotoDaGaleria(usuario.getId(), fotoId);
    }
}
