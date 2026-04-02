package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.EnderecoViaCepDTO;
import com.saas.clienthub.service.ViaCepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para consulta de CEP via API ViaCEP.
 *
 * =====================================================================
 * CONCEITO: Por que expor este endpoint na nossa API?
 * =====================================================================
 * O frontend JavaScript (app.js) chama /api/cep/{cep} quando o usuário
 * digita um CEP no formulário. Isso traz duas vantagens:
 *
 * 1. Evita CORS: o browser não pode chamar viacep.com.br diretamente
 *    (em alguns contextos). Nossa API funciona como proxy.
 *
 * 2. Centraliza a lógica: se quisermos trocar ViaCEP por outra API,
 *    alteramos só o ViaCepService — o frontend não muda nada.
 *
 * =====================================================================
 * CONCEITO: Proxy / BFF (Backend for Frontend)
 * =====================================================================
 * Este endpoint é um exemplo de padrão BFF: o backend expõe um endpoint
 * simplificado que o frontend consome, enquanto a complexidade da integração
 * com APIs externas fica encapsulada no servidor.
 */
@RestController
@RequestMapping("/api/cep")
@Tag(name = "CEP", description = "Consulta de endereço via CEP (ViaCEP)")
public class CepRestController {

    private final ViaCepService viaCepService;

    public CepRestController(ViaCepService viaCepService) {
        this.viaCepService = viaCepService;
    }

    /**
     * GET /api/cep/{cep} → consulta endereço pelo CEP.
     *
     * Exemplo: GET /api/cep/01001000
     * Resposta: { "cep": "01001000", "logradouro": "Praça da Sé", "bairro": "Sé", ... }
     *
     * Se o CEP não existir, o ViaCepService lança BusinessException → HTTP 422.
     */
    @GetMapping("/{cep}")
    @Operation(summary = "Consultar CEP", description = "Busca endereço pelo CEP usando a API ViaCEP")
    public ResponseEntity<EnderecoViaCepDTO> buscarCep(@PathVariable String cep) {
        return ResponseEntity.ok(viaCepService.buscarCep(cep));
    }
}
