package com.saas.clienthub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração do RestTemplate — cliente HTTP para consumir APIs externas.
 *
 * =====================================================================
 * CONCEITO: @Configuration e @Bean
 * =====================================================================
 * @Configuration → diz ao Spring que esta classe contém definições de beans.
 * @Bean          → diz ao Spring para gerenciar o objeto retornado pelo método.
 *
 * Quando alguma classe precisar de um RestTemplate (via injeção de dependência),
 * o Spring vai usar o objeto criado aqui — com as configurações de timeout.
 *
 * =====================================================================
 * CONCEITO: Por que configurar timeout?
 * =====================================================================
 * Sem timeout, se a API do ViaCEP ficar lenta ou fora do ar, a thread
 * do servidor ficaria travada esperando para sempre.
 *
 * Com timeout de 5 segundos:
 *   - connectTimeout → tempo máximo para estabelecer a conexão TCP
 *   - readTimeout    → tempo máximo para receber a resposta após conectar
 *
 * Se estourar, o Spring lança RestClientException, que capturamos no ViaCepService.
 *
 * =====================================================================
 * CONCEITO: Injeção de Dependência (IoC)
 * =====================================================================
 * Em vez de fazer "new RestTemplate()" em cada classe que precisa,
 * declaramos aqui e o Spring injeta onde precisarmos.
 * Isso facilita testes (podemos substituir por um mock) e centraliza a config.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Cria a factory que controla como as conexões HTTP são abertas
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Tempo máximo para conectar ao servidor remoto (ViaCEP): 5 segundos
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));

        // Tempo máximo para ler a resposta após conectado: 5 segundos
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory); // aplica as configurações de timeout
        return restTemplate;
    }
}
