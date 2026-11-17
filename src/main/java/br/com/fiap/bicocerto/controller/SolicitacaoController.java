package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.dto.*;
import br.com.fiap.bicocerto.model.StatusSolicitacao;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import br.com.fiap.bicocerto.service.AvaliacaoService;
import br.com.fiap.bicocerto.service.SolicitacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/solicitacoes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Solicitacoes", description = "Pedidos de servico: triagem com IA, atendimento e avaliacao")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;
    private final AvaliacaoService avaliacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService, AvaliacaoService avaliacaoService) {
        this.solicitacaoService = solicitacaoService;
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping("/triagem")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Previa da triagem com IA (categoria, urgencia e preco justo) sem criar o pedido")
    public TriagemResponse triagem(@RequestBody @Valid TriagemRequest request) {
        return solicitacaoService.simularTriagem(request.descricao(), request.observacao());
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Cria o pedido: a IA faz a triagem e os prestadores da categoria sao notificados via RabbitMQ")
    public ResponseEntity<SolicitacaoResponse> criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                     @RequestBody @Valid SolicitacaoRequest request) {
        SolicitacaoResponse criada = solicitacaoService.criar(usuario.getId(), request);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Pedidos do cliente logado")
    public PaginaResponse<SolicitacaoResponse> minhas(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @ParameterObject @PageableDefault(size = 10, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(solicitacaoService.minhas(usuario.getId(), pageable));
    }

    @GetMapping("/abertas")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Pedidos abertos da categoria do prestador logado")
    public PaginaResponse<SolicitacaoResponse> abertas(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @ParameterObject @PageableDefault(size = 10, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(solicitacaoService.abertasParaPrestador(usuario.getId(), pageable));
    }

    @GetMapping("/atendimentos")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Pedidos assumidos pelo prestador logado")
    public PaginaResponse<SolicitacaoResponse> atendimentos(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @ParameterObject @PageableDefault(size = 10, sort = "atualizadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(solicitacaoService.meusAtendimentos(usuario.getId(), pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Todos os pedidos, com filtro opcional por status (ADMIN)")
    public PaginaResponse<SolicitacaoResponse> todas(@RequestParam(required = false) StatusSolicitacao status,
            @ParameterObject @PageableDefault(size = 10, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(solicitacaoService.todas(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do pedido (dono, prestador da categoria/atendimento ou ADMIN)")
    public SolicitacaoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitacaoService.buscar(id, usuario);
    }

    @GetMapping("/{id}/fotos")
    @Operation(summary = "Fotos do problema (mesmas regras de acesso do detalhe do pedido)")
    public java.util.List<FotoResponse> fotos(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitacaoService.fotos(id, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Edita a descricao de um pedido ABERTO (a triagem e refeita)")
    public SolicitacaoResponse atualizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @RequestBody @Valid SolicitacaoRequest request) {
        return solicitacaoService.atualizar(id, usuario.getId(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Cancela o pedido (exclusao logica: status CANCELADA), com motivo opcional")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @RequestParam(required = false)
                                         @jakarta.validation.constraints.Size(max = 200, message = "O motivo deve ter ate 200 caracteres")
                                         String motivo) {
        solicitacaoService.cancelar(id, usuario.getId(), motivo);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/contratacao")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Cliente registra o prestador contratado (orcamento aceito no modulo .NET); libera endereco e telefone")
    public SolicitacaoResponse registrarContratacao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                    @RequestBody @Valid ContratacaoRegistroRequest request) {
        return solicitacaoService.registrarContratacao(id, usuario.getId(), request.prestadorUsuarioId());
    }

    @PatchMapping("/{id}/assumir")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Prestador assume o pedido aberto da sua categoria")
    public SolicitacaoResponse assumir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitacaoService.assumir(id, usuario.getId());
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Prestador marca o servico como concluido")
    public SolicitacaoResponse concluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitacaoService.concluir(id, usuario.getId());
    }

    @PostMapping("/{id}/avaliacao")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Cliente avalia (1 a 5) o prestador de um pedido concluido")
    public ResponseEntity<AvaliacaoResponse> avaliar(@PathVariable Long id,
                                                     @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                     @RequestBody @Valid AvaliacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacaoService.avaliar(id, usuario.getId(), request));
    }
}
