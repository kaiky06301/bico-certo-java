package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.dto.*;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import br.com.fiap.bicocerto.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conta")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Minha conta", description = "Dados pessoais, senha, avaliacoes feitas e exclusao da conta do usuario logado")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
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

    @DeleteMapping
    @Operation(summary = "Exclui a conta (pede a senha; apaga os dados pessoais e encerra o acesso)")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @RequestBody @Valid ExcluirContaRequest request) {
        contaService.excluir(usuario.getId(), request.senha());
        return ResponseEntity.noContent().build();
    }
}
