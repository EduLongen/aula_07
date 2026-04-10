package com.saas.clienthub.controller.rest;

import com.saas.clienthub.model.dto.LoginRequestDTO;
import com.saas.clienthub.model.dto.LoginResponseDTO;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.UsuarioRepository;
import com.saas.clienthub.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para autenticação via JWT.
 *
 * =====================================================================
 * CONCEITO: Fluxo completo de autenticação JWT
 * =====================================================================
 *
 * 1. Cliente envia POST /api/auth/login com email e senha (JSON)
 *
 * 2. Este controller:
 *    a) Busca o usuário pelo email no banco
 *    b) Verifica se o usuário está ativo
 *    c) Compara a senha digitada com o hash BCrypt do banco
 *    d) Se tudo OK: gera um JWT e retorna ao cliente
 *    e) Se falhar: retorna HTTP 401 Unauthorized
 *
 * 3. Cliente armazena o token (localStorage, variável, etc.)
 *
 * 4. Nas próximas requisições, envia o token no header:
 *    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 *
 * 5. O JwtAuthenticationFilter intercepta, valida o token
 *    e autentica o usuário automaticamente
 *
 * =====================================================================
 * OBS: Poderiamos usar o AuthenticationManager do Spring Security, 
 * o authenticationManager.authenticate() que delega
 * para o CustomUserDetailsService.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login e obtenção de token JWT para acesso à API")
public class AuthRestController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthRestController(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/auth/login → autentica o usuário e retorna um JWT.
     *
     * Exemplo de requisição (curl):
     *   curl -X POST http://localhost:8080/api/auth/login \
     *     -H "Content-Type: application/json" \
     *     -d '{"email": "admin@clienthub.com", "senha": "admin123"}'
     *
     * Exemplo de resposta (200 OK):
     *   {
     *     "token": "eyJhbGciOiJIUzI1NiJ9...",
     *     "tipo": "Bearer",
     *     "email": "admin@clienthub.com",
     *     "nome": "Administrador",
     *     "role": "ADMIN"
     *   }
     *
     * Exemplo de uso do token nas próximas requisições:
     *   curl http://localhost:8080/api/empresas \
     *     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login (JWT)",
            description = "Autentica o usuário e retorna um token JWT. " +
                    "Use o token nas próximas requisições no header: Authorization: Bearer <token>"
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
                    java.util.Map.of("erro", "Email ou senha inválidos")
            );
        }

        // 3. Gera o token JWT com email e role
        String token = jwtService.gerarToken(
                usuario.getEmail(),
                usuario.getRole().name()
        );

        // 4. Retorna o token + dados básicos do usuário
        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .role(usuario.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }
}
