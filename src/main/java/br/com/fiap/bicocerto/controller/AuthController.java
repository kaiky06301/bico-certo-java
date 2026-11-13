package br.com.fiap.bicocerto.controller;

import br.com.fiap.bicocerto.dto.LoginRequest;
import br.com.fiap.bicocerto.dto.LoginResponse;
import br.com.fiap.bicocerto.dto.RegistroRequest;
import br.com.fiap.bicocerto.dto.UsuarioResponse;
import br.com.fiap.bicocerto.security.UsuarioAutenticado;
import br.com.fiap.bicocerto.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao", description = "Cadastro, login (JWT) e perfil do usuario logado")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login: devolve o token JWT (Bearer) e os dados do usuario")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/registrar")
    @Operation(summary = "Cadastro publico de CLIENTE ou PRESTADOR (prestador informa categoriaId)")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody @Valid RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Dados do usuario logado")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return authService.perfil(usuario.getId());
    }
}
