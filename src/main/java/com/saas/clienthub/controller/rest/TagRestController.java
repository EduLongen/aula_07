package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.TagRequestDTO;
import com.saas.clienthub.model.dto.TagResponseDTO;
import com.saas.clienthub.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST para operações de Tag via API JSON.
 *
 * URL base: /api/empresas/{empresaId}/tags — recurso aninhado por empresa,
 * reforçando o isolamento multi-tenant diretamente na URL.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/tags")
@Tag(name = "Tags", description = "Gestão de tags por empresa")
public class TagRestController {

    private final TagService tagService;

    public TagRestController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "Listar tags", description = "Retorna todas as tags de uma empresa")
    public ResponseEntity<List<TagResponseDTO>> listar(@PathVariable Long empresaId) {
        return ResponseEntity.ok(tagService.listarPorEmpresa(empresaId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tag por ID", description = "Retorna os dados de uma tag específica")
    public ResponseEntity<TagResponseDTO> buscarPorId(@PathVariable Long empresaId,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(tagService.buscarPorId(empresaId, id));
    }

    @PostMapping
    @Operation(summary = "Criar tag", description = "Cria uma nova tag na empresa")
    public ResponseEntity<TagResponseDTO> criar(@PathVariable Long empresaId,
                                                @Valid @RequestBody TagRequestDTO dto) {
        TagResponseDTO tag = tagService.criar(empresaId, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(tag.getId())
                .toUri();
        return ResponseEntity.created(location).body(tag);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tag", description = "Atualiza nome e cor de uma tag existente")
    public ResponseEntity<TagResponseDTO> atualizar(@PathVariable Long empresaId,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody TagRequestDTO dto) {
        return ResponseEntity.ok(tagService.atualizar(empresaId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar tag", description = "Desativa uma tag (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long empresaId, @PathVariable Long id) {
        tagService.desativar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
