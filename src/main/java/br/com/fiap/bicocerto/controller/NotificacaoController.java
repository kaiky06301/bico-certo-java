package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.dto.NotificacaoResponse;
import br.com.fiap.bicocerto.dto.PaginaResponse;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import br.com.fiap.bicocerto.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificacoes", description = "Avisos gerados de forma assincrona pelo consumidor RabbitMQ")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    @Operation(summary = "Notificacoes do usuario logado (filtro opcional: lida=true/false)")
    public PaginaResponse<NotificacaoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Boolean lida,
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(notificacaoService.listar(usuario.getId(), lida, pageable));
    }

    @GetMapping("/nao-lidas")
    @Operation(summary = "Quantidade de notificacoes nao lidas (badge do app)")
    public Map<String, Long> naoLidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("naoLidas", notificacaoService.naoLidas(usuario.getId()));
    }

    @PatchMapping("/lidas")
    @Operation(summary = "Marca todas as notificacoes como lidas")
    public Map<String, Integer> marcarTodas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("atualizadas", notificacaoService.marcarTodasComoLidas(usuario.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma notificacao")
    public org.springframework.http.ResponseEntity<Void> excluir(@PathVariable Long id,
                                                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        notificacaoService.excluir(id, usuario.getId());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @DeleteMapping("/lidas")
    @Operation(summary = "Limpa as notificacoes ja lidas")
    public Map<String, Integer> limparLidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("removidas", notificacaoService.limparLidas(usuario.getId()));
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marca uma notificacao como lida")
    public NotificacaoResponse marcarComoLida(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notificacaoService.marcarComoLida(id, usuario.getId());
    }
}
