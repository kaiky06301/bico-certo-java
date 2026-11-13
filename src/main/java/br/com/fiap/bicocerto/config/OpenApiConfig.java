package br.com.fiap.bicocerto.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Bico Certo API",
        version = "1.0",
        description = "CitizenTech que conecta moradores a prestadores de servico do bairro, "
                + "com triagem inteligente do problema e faixa de preco justo. "
                + "Faca login em /auth/login e use o token no botao Authorize."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
