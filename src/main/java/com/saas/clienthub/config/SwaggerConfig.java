package com.saas.clienthub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
 * CONCEITO: OpenAPI Specification
 * =====================================================================
 * OpenAPI é um padrão da indústria para descrever APIs REST em formato JSON/YAML.
 * O arquivo gerado fica em: http://localhost:8080/api-docs
 * A interface visual fica em: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClientHub SaaS API")
                        .version("1.0")
                        .description("API REST multi-tenant para gestão de clientes. " +
                                "Cada empresa (tenant) possui sua base de clientes isolada."));
    }
}
