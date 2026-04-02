package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.ClienteRequestDTO;
import com.saas.clienthub.model.dto.ClienteResponseDTO;
import com.saas.clienthub.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST para operações de Cliente via API JSON.
 *
 * =====================================================================
 * CONCEITO: URLs RESTful com Recursos Aninhados
 * =====================================================================
 * A URL base é: /api/empresas/{empresaId}/clientes
 *
 * Isso modela o relacionamento hierárquico REST:
 *   Clientes são SUB-RECURSOS de Empresas
 *
 * Exemplos:
 *   GET  /api/empresas/1/clientes         → lista clientes da empresa 1
 *   GET  /api/empresas/1/clientes/5       → cliente 5 da empresa 1
 *   POST /api/empresas/1/clientes         → cria cliente na empresa 1
 *
 * Vantagens desta abordagem:
 *   1. A URL já carrega o contexto do tenant (empresaId)
 *   2. Impossível acessar cliente de outra empresa "por engano"
 *   3. Semântica clara: "clientes desta empresa"
 *
 * =====================================================================
 * CONCEITO: @RequestParam vs @PathVariable
 * =====================================================================
 * @PathVariable  → extrai parte da URL:  /clientes/{id}  → id
 * @RequestParam  → extrai query string:  /pesquisa?nome=João  → nome
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/clientes")
@Tag(name = "Clientes", description = "Operações de gestão de clientes por empresa")
public class ClienteRestController {

    private final ClienteService clienteService;

    public ClienteRestController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * GET /api/empresas/{empresaId}/clientes → lista todos os clientes da empresa.
     * O empresaId garante que só retornamos clientes do tenant correto.
     */
    @GetMapping
    @Operation(summary = "Listar clientes", description = "Retorna todos os clientes de uma empresa")
    public ResponseEntity<List<ClienteResponseDTO>> listar(@PathVariable Long empresaId) {
        return ResponseEntity.ok(clienteService.listarPorEmpresa(empresaId));
    }

    /**
     * GET /api/empresas/{empresaId}/clientes/{id} → busca um cliente específico.
     * Ambos os IDs são necessários para garantir isolamento multi-tenant.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID", description = "Retorna os dados de um cliente específico")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(
            @PathVariable Long empresaId,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(empresaId, id));
    }

    /**
     * GET /api/empresas/{empresaId}/clientes/pesquisa?nome=Ana
     * Pesquisa por nome parcial — ex: "Ana" encontra "Ana Silva", "Adriana", etc.
     *
     * @RequestParam(required = false) permitiria que o parâmetro fosse opcional.
     * Aqui é obrigatório por padrão.
     */
    @GetMapping("/pesquisa")
    @Operation(summary = "Pesquisar clientes por nome",
               description = "Pesquisa clientes pelo nome dentro de uma empresa")
    public ResponseEntity<List<ClienteResponseDTO>> pesquisar(
            @PathVariable Long empresaId,
            @RequestParam String nome) {
        return ResponseEntity.ok(clienteService.pesquisarPorNome(empresaId, nome));
    }

    /**
     * POST /api/empresas/{empresaId}/clientes → cria novo cliente na empresa.
     * Retorna HTTP 201 Created com Location header apontando para o novo recurso.
     */
    @PostMapping
    @Operation(summary = "Criar cliente", description = "Cadastra um novo cliente em uma empresa")
    public ResponseEntity<ClienteResponseDTO> criar(
            @PathVariable Long empresaId,
            @Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO cliente = clienteService.criar(empresaId, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cliente.getId())
                .toUri();
        return ResponseEntity.created(location).body(cliente);
    }

    /** PUT /api/empresas/{empresaId}/clientes/{id} → atualiza cliente */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente", description = "Atualiza os dados de um cliente existente")
    public ResponseEntity<ClienteResponseDTO> atualizar(
            @PathVariable Long empresaId,
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(empresaId, id, dto));
    }

    /** DELETE /api/empresas/{empresaId}/clientes/{id} → desativa cliente (soft delete) */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cliente", description = "Desativa um cliente (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long empresaId, @PathVariable Long id) {
        clienteService.desativar(empresaId, id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
