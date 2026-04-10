package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.UsuarioRequestDTO;
import com.saas.clienthub.model.dto.UsuarioResponseDTO;
import com.saas.clienthub.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST para operações de Usuário via API JSON.
 *
 * =====================================================================
 * CONCEITO: API de Gestão de Usuários
 * =====================================================================
 * Esta API permite gerenciar os usuários do sistema (CRUD completo).
 * Cada usuário possui:
 *   - email (username do login)
 *   - senha (armazenada como hash BCrypt — nunca em texto puro)
 *   - role (ADMIN, GESTOR ou USUARIO)
 *   - empresa vinculada (obrigatória para GESTOR/USUARIO, null para ADMIN)
 *
 * Regras de negócio validadas no UsuarioService:
 *   - Email deve ser único no sistema
 *   - ADMIN não pode ter empresa vinculada
 *   - GESTOR/USUARIO devem ter empresa vinculada
 *   - Senha mínima de 6 caracteres (na criação)
 *   - Na edição, senha vazia mantém a senha atual
 *
 * =====================================================================
 * CONCEITO: Autenticação da API
 * =====================================================================
 * Atualmente a API (/api/**) é pública (permitAll no SecurityConfig).
 * Em uma versão futura, será protegida com JWT (JSON Web Token).
 * Por enquanto, qualquer cliente (Postman, curl, frontend) pode acessar.
 */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Operações de gestão de usuários do sistema")
public class UsuarioRestController {

    private final UsuarioService usuarioService;

    public UsuarioRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * GET /api/usuarios → lista todos os usuários.
     * Retorna HTTP 200 com a lista de usuários em JSON.
     * Nota: a senha NUNCA é retornada no DTO de resposta.
     */
    @GetMapping
    @Operation(summary = "Listar usuários", description = "Retorna todos os usuários cadastrados no sistema")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    /**
     * GET /api/usuarios/{id} → retorna um usuário específico.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados de um usuário específico")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    /**
     * POST /api/usuarios → cria um novo usuário.
     *
     * Exemplo de JSON para criação:
     * {
     *   "nome": "João Silva",
     *   "email": "joao@empresa.com",
     *   "senha": "senha123",
     *   "role": "GESTOR",
     *   "empresaId": 1
     * }
     *
     * A senha é criptografada com BCrypt antes de salvar no banco.
     * Retorna HTTP 201 Created com o header Location apontando para o recurso.
     */
    @PostMapping
    @Operation(summary = "Criar usuário", description = "Cadastra um novo usuário no sistema. A senha será criptografada com BCrypt.")
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO usuario = usuarioService.criar(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(usuario.getId())
                .toUri();

        return ResponseEntity.created(location).body(usuario);
    }

    /**
     * PUT /api/usuarios/{id} → atualiza um usuário existente.
     *
     * Regra especial: se o campo "senha" vier vazio ou null,
     * a senha atual do banco é mantida (não precisa reenviar).
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário. Envie senha vazia para manter a atual.")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    /**
     * DELETE /api/usuarios/{id} → desativa um usuário (soft delete).
     * O usuário não é removido do banco — apenas marcado como inativo.
     * Um usuário inativo não consegue fazer login.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar usuário", description = "Desativa um usuário (soft delete). Usuários inativos não conseguem fazer login.")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
