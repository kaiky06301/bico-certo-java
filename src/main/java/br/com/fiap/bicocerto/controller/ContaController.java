package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.dto.*;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import br.com.fiap.bicocerto.service.ContaService;
import br.com.fiap.bicocerto.service.EnderecoService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conta")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Minha conta", description = "Dados pessoais, enderecos, senha, avaliacoes feitas e exclusao da conta do usuario logado")
public class ContaController {

    private final ContaService contaService;
    private final EnderecoService enderecoService;

    public ContaController(ContaService contaService, EnderecoService enderecoService) {
        this.contaService = contaService;
        this.enderecoService = enderecoService;
    }

    @GetMapping
    @Operation(summary = "Dados do usuario logado")
    public UsuarioResponse minhaConta(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return contaService.minhaConta(usuario.getId());
    }

    @PutMapping
    @Operation(summary = "Atualiza nome, telefone, bairro e endereco")
    public UsuarioResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @RequestBody @Valid ContaRequest request) {
        return contaService.atualizar(usuario.getId(), request);
    }

    @PutMapping("/senha")
    @Operation(summary = "Troca a senha (exige a senha atual)")
    public ResponseEntity<Void> alterarSenha(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @RequestBody @Valid AlterarSenhaRequest request) {
        contaService.alterarSenha(usuario.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avaliacoes")
    @Operation(summary = "Avaliacoes que o usuario fez dos prestadores")
    public PaginaResponse<AvaliacaoResponse> avaliacoesFeitas(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.from(contaService.avaliacoesFeitas(usuario.getId(), pageable));
    }

    // ---------- meus enderecos ----------

    @GetMapping("/enderecos")
    @Operation(summary = "Enderecos salvos (o principal vem primeiro)")
    public List<EnderecoResponse> enderecos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return enderecoService.listar(usuario.getId());
    }

    @PostMapping("/enderecos")
    @Operation(summary = "Salva um endereco (o primeiro vira principal; maximo de 10)")
    public ResponseEntity<EnderecoResponse> criarEndereco(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                          @RequestBody @Valid EnderecoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoService.criar(usuario.getId(), request));
    }

    @PutMapping("/enderecos/{id}")
    @Operation(summary = "Edita um endereco salvo")
    public EnderecoResponse atualizarEndereco(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                              @RequestBody @Valid EnderecoRequest request) {
        return enderecoService.atualizar(usuario.getId(), id, request);
    }

    @PatchMapping("/enderecos/{id}/principal")
    @Operation(summary = "Marca o endereco como principal")
    public EnderecoResponse definirPrincipal(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return enderecoService.definirPrincipal(usuario.getId(), id);
    }

    @DeleteMapping("/enderecos/{id}")
    @Operation(summary = "Exclui um endereco (se era o principal, o proximo vira principal)")
    public ResponseEntity<Void> excluirEndereco(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        enderecoService.excluir(usuario.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Exclui a conta (pede a senha; apaga os dados pessoais e encerra o acesso)")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @RequestBody @Valid ExcluirContaRequest request) {
        contaService.excluir(usuario.getId(), request.senha());
        return ResponseEntity.noContent().build();
    }
}
