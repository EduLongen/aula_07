package com.saas.clienthub.service;

import com.saas.clienthub.exception.BusinessException;
import com.saas.clienthub.model.dto.EnderecoViaCepDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service responsável por consultar endereços via API pública ViaCEP.
 *
 * =====================================================================
 * CONCEITO: @Service
 * =====================================================================
 * @Service é um estereótipo do Spring que marca esta classe como
 * componente de lógica de negócio. O Spring a detecta automaticamente
 * e a registra no contexto de aplicação (ApplicationContext), tornando-a
 * disponível para injeção de dependência em outros componentes.
 *
 * Hierarquia dos estereótipos:
 *   @Component      → componente genérico
 *   @Service        → lógica de negócio (camada de serviço)
 *   @Repository     → acesso a dados
 *   @Controller     → camada web
 *   @RestController → camada web REST (= @Controller + @ResponseBody)
 *
 * =====================================================================
 * CONCEITO: Injeção via Construtor
 * =====================================================================
 * Em vez de @Autowired no campo (field injection), usamos injeção via
 * construtor. Vantagens:
 *   - Campos podem ser final (imutáveis após criação)
 *   - Facilita testes unitários (podemos passar mocks manualmente)
 *   - Erros de dependência circular são detectados na inicialização
 *
 * =====================================================================
 * CONCEITO: API ViaCEP
 * =====================================================================
 * ViaCEP é uma API pública gratuita que retorna dados de endereço dado um CEP.
 * Endpoint: https://viacep.com.br/ws/{cep}/json/
 * Resposta de sucesso: { "cep": "01001000", "logradouro": "Praça da Sé", ... }
 * CEP inválido: { "erro": true }
 */
@Service
public class ViaCepService {

    private final RestTemplate restTemplate;

    /**
     * Injeção via construtor — o Spring injeta o RestTemplate configurado em RestTemplateConfig.
     * O RestTemplate já vem com timeout de 5 segundos configurado.
     */
    public ViaCepService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Consulta o endereço de um CEP na API ViaCEP.
     *
     * @param cep CEP em qualquer formato (com ou sem traço/espaço)
     * @return EnderecoViaCepDTO com os dados do endereço
     * @throws BusinessException se CEP não encontrado ou erro de conexão
     */
    public EnderecoViaCepDTO buscarCep(String cep) {
        // Remove tudo que não for dígito (ex: "01001-000" → "01001000")
        String cepLimpo = cep.replaceAll("[^\\d]", "");

        try {
            // RestTemplate.getForObject() faz um GET HTTP e desserializa o JSON
            // automaticamente para EnderecoViaCepDTO usando Jackson (biblioteca JSON)
            EnderecoViaCepDTO endereco = restTemplate.getForObject(
                    "https://viacep.com.br/ws/" + cepLimpo + "/json/",
                    EnderecoViaCepDTO.class
            );

            // A API ViaCEP retorna { "erro": true } quando o CEP não existe
            // em vez de retornar HTTP 404 — por isso checamos o campo "erro"
            if (endereco == null || Boolean.TRUE.equals(endereco.getErro())) {
                throw new BusinessException("CEP não encontrado: " + cep);
            }

            return endereco;

        } catch (RestClientException e) {
            // RestClientException cobre timeouts, erros de conexão, HTTP 5xx, etc.
            // Convertemos para BusinessException com mensagem amigável
            throw new BusinessException("Erro ao consultar ViaCEP");
        }
    }
}
