package com.saas.clienthub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Swagger / OpenAPI — documentação interativa da API REST.
 *
 * =====================================================================
 * CONCEITO: Swagger / SpringDoc OpenAPI
 * =====================================================================
 * O SpringDoc escaneia automaticamente todos os @RestController e gera
 * um arquivo JSON com a especificação OpenAPI 3.x da nossa API.
 *
 * Com isso, o Swagger UI disponível em /swagger-ui.html permite:
 *   - Ver todos os endpoints disponíveis
 *   - Entender os parâmetros e respostas esperados
 *   - Testar as requisições diretamente pelo navegador (sem Postman)
 *
 * As anotações @Tag e @Operation nos controllers enriquecem a documentação
 * com descrições legíveis para humanos.
 *
 * =====================================================================
 * CONCEITO: SecurityScheme no Swagger
 * =====================================================================
 * Para testar endpoints protegidos no Swagger UI, precisamos configurar
 * o esquema de autenticação. Adicionamos um SecurityScheme do tipo
 * "bearer" (JWT) que cria o botão "Authorize" no Swagger UI.
 *
 * Fluxo para testar no Swagger:
 * 1. Chame POST /api/auth/login para obter o token
 * 2. Clique em "Authorize" no topo da página
 * 3. Cole o token (sem o prefixo "Bearer")
 * 4. Agora todas as requisições incluem o header Authorization automaticamente
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Nome do esquema de segurança (referenciado no SecurityRequirement)
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("ClientHub SaaS API")
                        .version("1.0")
                        .description("API REST multi-tenant para gestão de clientes. " +
                                "Cada empresa (tenant) possui sua base de clientes isolada.\n\n" +
                                "**Autenticação:** Use `POST /api/auth/login` para obter um token JWT, " +
                                "depois clique em **Authorize** e insira o token."))

                // Adiciona o requisito de segurança globalmente
                // (todos os endpoints mostram o cadeado no Swagger UI)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                // Define o esquema de segurança JWT Bearer
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)     // Tipo HTTP (header)
                                        .scheme("bearer")                    // Esquema Bearer
                                        .bearerFormat("JWT")                 // Formato JWT
                                        .description("Insira o token JWT obtido em /api/auth/login")
                        ));
    }
}
