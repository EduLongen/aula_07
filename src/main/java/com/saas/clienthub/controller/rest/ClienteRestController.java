package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.ClienteRequestDTO;
import com.saas.clienthub.model.dto.ClienteResponseDTO;
import com.saas.clienthub.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controller REST para operações de Cliente via API JSON.
 *
 * A listagem agora é paginada — recebe page, size e sort como query params.
 * Retorna Page<ClienteResponseDTO>, que inclui conteúdo + metadados (totalPages, totalElements, etc).
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
     * GET /api/empresas/{empresaId}/clientes?page=0&size=10&sort=nome
     * Parâmetros de paginação opcionais — defaults: página 0, 10 itens, ordenado por nome.
     */
    @GetMapping
    @Operation(summary = "Listar clientes paginados",
               description = "Retorna clientes de uma empresa com paginação e ordenação")
    public ResponseEntity<Page<ClienteResponseDTO>> listar(
            @PathVariable Long empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nome") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(clienteService.listarPorEmpresa(empresaId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long empresaId,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(empresaId, id));
    }

    @GetMapping("/pesquisa")
    @Operation(summary = "Pesquisar clientes por nome (paginado)")
    public ResponseEntity<Page<ClienteResponseDTO>> pesquisar(
            @PathVariable Long empresaId,
            @RequestParam String nome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nome") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(clienteService.pesquisarPorNome(empresaId, nome, pageable));
    }

    @PostMapping
    @Operation(summary = "Criar cliente")
    public ResponseEntity<ClienteResponseDTO> criar(@PathVariable Long empresaId,
                                                    @Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO cliente = clienteService.criar(empresaId, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cliente.getId())
                .toUri();
        return ResponseEntity.created(location).body(cliente);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable Long empresaId,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody ClienteRequestDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(empresaId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cliente")
    public ResponseEntity<Void> desativar(@PathVariable Long empresaId, @PathVariable Long id) {
        clienteService.desativar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
