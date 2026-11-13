package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.dto.AvaliacaoResponse;
import br.com.fiap.bicocerto.dto.PaginaResponse;
import br.com.fiap.bicocerto.dto.PrestadorPerfilRequest;
import br.com.fiap.bicocerto.dto.PrestadorResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Atualiza categoria, descricao, bairro e telefone do prestador logado")
    public PrestadorResponse atualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @RequestBody @Valid PrestadorPerfilRequest request) {
        return prestadorService.atualizarMeuPerfil(usuario.getId(), request);
    }
}
