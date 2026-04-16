package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.LoginRequestDTO;
import com.saas.clienthub.model.dto.LoginResponseDTO;
import com.saas.clienthub.model.dto.RefreshTokenRequestDTO;
import com.saas.clienthub.model.entity.RefreshToken;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.UsuarioRepository;
import com.saas.clienthub.security.JwtService;
import com.saas.clienthub.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller REST para autenticação via JWT + Refresh Token.
 *
 * =====================================================================
 * CONCEITO: Fluxo completo de autenticação (Access + Refresh Token)
 * =====================================================================
 *
 * 1. LOGIN
 *    POST /api/auth/login  { email, senha }
 *    → retorna { token (JWT curto), refreshToken (UUID longo) }
 *
 * 2. USO NORMAL
 *    Cliente envia o JWT no header Authorization: Bearer <token>
 *    em cada requisição à API
 *
 * 3. RENOVAÇÃO (quando o JWT expirar)
 *    POST /api/auth/refresh  { refreshToken }
 *    → retorna novo { token, refreshToken }
 *    → o refreshToken anterior é REVOGADO (rotação)
 *
 * 4. LOGOUT
 *    POST /api/auth/logout  { refreshToken }
 *    → revoga o refresh token no banco
 *    → o JWT ainda vale até expirar, mas não pode mais ser renovado
 *
 * =====================================================================
 * SEGURANÇA: Detecção de Reuso
 * =====================================================================
 * Se alguém tentar usar um refreshToken já revogado (porque foi roubado
 * ou porque houve um replay attack), o service revoga TODOS os refresh
 * tokens ativos do usuário e força novo login.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login, renovação de token (refresh) e logout")
public class AuthRestController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthRestController(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService,
                              RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * POST /api/auth/login → autentica o usuário e retorna access + refresh token.
     *
     * Exemplo de requisição (curl):
     *   curl -X POST http://localhost:8080/api/auth/login \
     *     -H "Content-Type: application/json" \
     *     -d '{"email": "admin@clienthub.com", "senha": "admin123"}'
     *
     * Exemplo de resposta (200 OK):
     *   {
     *     "token": "eyJhbGciOiJIUzI1NiJ9...",
     *     "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
     *     "tipo": "Bearer",
     *     "email": "admin@clienthub.com",
     *     "nome": "Administrador",
     *     "role": "ADMIN"
     *   }
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login (JWT + Refresh Token)",
            description = "Autentica o usuário e retorna um access token JWT (curta duração) " +
                    "e um refresh token (longa duração). Use o access token no header " +
                    "Authorization: Bearer <token>. Quando expirar, use o refresh token em /api/auth/refresh."
    )
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        // 1. Busca o usuário pelo email
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElse(null);

        // 2. Valida: usuário existe, está ativo e a senha bate
        if (usuario == null || !usuario.getAtivo()
                || !passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            // Retorna 401 com mensagem genérica (não revelar se o email existe ou não)
            return ResponseEntity.status(401).body(
                    Map.of("erro", "Email ou senha inválidos")
            );
        }

        // 3. Gera o access token JWT
        String token = jwtService.gerarToken(
                usuario.getEmail(),
                usuario.getRole().name()
        );

        // 4. Cria o refresh token e persiste no banco
        RefreshToken refreshToken = refreshTokenService.criarRefreshToken(usuario);

        // 5. Retorna o par de tokens + dados básicos do usuário
        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .role(usuario.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh → renova o access token usando o refresh token.
     *
     * Cada uso do refresh token o REVOGA e emite um novo (rotação).
     * Se o refresh token já estiver revogado, todos os tokens do usuário
     * são revogados por segurança (detecção de reuso).
     *
     * Exemplo de requisição:
     *   curl -X POST http://localhost:8080/api/auth/refresh \
     *     -H "Content-Type: application/json" \
     *     -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000"}'
     *
     * Exemplo de resposta (200 OK): mesmo formato do login, com tokens novos.
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar tokens (Refresh Token)",
            description = "Recebe um refresh token válido e retorna um novo par de tokens " +
                    "(access + refresh). O refresh token anterior é invalidado (rotação)."
    )
    public ResponseEntity<LoginResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        LoginResponseDTO response = refreshTokenService.renovarToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout → revoga o refresh token.
     *
     * O access token JWT continua válido até expirar (não há como revogá-lo
     * sem manter uma blacklist), mas não poderá ser renovado.
     *
     * Exemplo de requisição:
     *   curl -X POST http://localhost:8080/api/auth/logout \
     *     -H "Content-Type: application/json" \
     *     -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000"}'
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout (revogar refresh token)",
            description = "Revoga o refresh token informado. O access token JWT continua válido " +
                    "até expirar, mas não poderá ser renovado."
    )
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        refreshTokenService.revogarToken(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("mensagem", "Logout realizado com sucesso"));
    }
}
