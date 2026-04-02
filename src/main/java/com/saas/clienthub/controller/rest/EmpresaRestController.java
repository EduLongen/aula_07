package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.EmpresaRequestDTO;
import com.saas.clienthub.model.dto.EmpresaResponseDTO;
import com.saas.clienthub.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST para operações de Empresa via API JSON.
 *
 * =====================================================================
 * CONCEITO: @RestController vs @Controller
 * =====================================================================
 * @Controller      → retorna o nome de um template HTML (Thymeleaf)
 * @RestController  → retorna dados JSON diretamente no body da resposta
 *                    (equivalente a @Controller + @ResponseBody em todos os métodos)
 *
 * =====================================================================
 * CONCEITO: @RequestMapping
 * =====================================================================
 * Define a URL base de todos os endpoints desta classe: /api/empresas
 * Métodos internos completam a URL: GET /api/empresas, GET /api/empresas/{id}, etc.
 *
 * =====================================================================
 * CONCEITO: Swagger Annotations
 * =====================================================================
 * @Tag     → agrupa endpoints no Swagger UI (aparece como seção "Empresas")
 * @Operation → descreve o endpoint individualmente na documentação
 *
 * =====================================================================
 * CONCEITO: ResponseEntity
 * =====================================================================
 * Permite controlar o HTTP status code, headers e body da resposta.
 * ResponseEntity.ok(body)      → 200 OK
 * ResponseEntity.created(uri)  → 201 Created
 * ResponseEntity.noContent()   → 204 No Content
 */
@RestController
@RequestMapping("/api/empresas")
@Tag(name = "Empresas", description = "Operações de gestão de empresas (tenants)")
public class EmpresaRestController {

    private final EmpresaService empresaService;

    /** Injeção via construtor — Spring injeta EmpresaService automaticamente */
    public EmpresaRestController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    /**
     * GET /api/empresas → lista todas as empresas.
     * ResponseEntity.ok() retorna HTTP 200 com o JSON no body.
     */
    @GetMapping
    @Operation(summary = "Listar empresas", description = "Retorna todas as empresas cadastradas")
    public ResponseEntity<List<EmpresaResponseDTO>> listar() {
        return ResponseEntity.ok(empresaService.listarTodas());
    }

    /**
     * GET /api/empresas/{id} → retorna uma empresa específica.
     * @PathVariable extrai o valor {id} da URL e passa para o método.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar empresa por ID", description = "Retorna os dados de uma empresa específica")
    public ResponseEntity<EmpresaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.buscarPorId(id));
    }

    /**
     * POST /api/empresas → cria uma nova empresa.
     *
     * @Valid         → aciona o Bean Validation no DTO. Se falhar, lança
     *                  MethodArgumentNotValidException → HTTP 400 (tratado no GlobalExceptionHandler)
     * @RequestBody   → desserializa o JSON do body da requisição para EmpresaRequestDTO
     *
     * Boas práticas REST: POST bem-sucedido retorna HTTP 201 Created com:
     *   - Body: dados do recurso criado
     *   - Header Location: URL do novo recurso (ex: /api/empresas/42)
     */
    @PostMapping
    @Operation(summary = "Criar empresa", description = "Cadastra uma nova empresa no sistema")
    public ResponseEntity<EmpresaResponseDTO> criar(@Valid @RequestBody EmpresaRequestDTO dto) {
        EmpresaResponseDTO empresa = empresaService.criar(dto);

        // Constrói a URL do recurso criado para o header Location
        // Ex: se a requisição foi para /api/empresas e o id gerado foi 42,
        // o header Location será /api/empresas/42
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(empresa.getId())
                .toUri();

        return ResponseEntity.created(location).body(empresa);
    }

    /**
     * PUT /api/empresas/{id} → atualiza uma empresa existente.
     * PUT substitui o recurso completo (todos os campos devem ser enviados).
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar empresa", description = "Atualiza os dados de uma empresa existente")
    public ResponseEntity<EmpresaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EmpresaRequestDTO dto) {
        return ResponseEntity.ok(empresaService.atualizar(id, dto));
    }

    /**
     * DELETE /api/empresas/{id} → desativa uma empresa (soft delete).
     * HTTP 204 No Content → sucesso sem body de resposta (padrão REST para DELETE).
     *
     * Nota: usamos soft delete (ativa=false) em vez de DELETE real no banco,
     * preservando o histórico de dados.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar empresa", description = "Desativa uma empresa (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        empresaService.desativar(id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
