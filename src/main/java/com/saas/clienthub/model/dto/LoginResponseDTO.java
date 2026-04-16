package com.saas.clienthub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta do login e do refresh na API REST.
 *
 * Retorna o access token JWT, o refresh token e informações básicas do usuário:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
 *   "tipo": "Bearer",
 *   "email": "admin@clienthub.com",
 *   "nome": "Administrador",
 *   "role": "ADMIN"
 * }
 *
 * - token:        JWT de curta duração, usado no header Authorization
 * - refreshToken: UUID opaco de longa duração, usado APENAS em /api/auth/refresh
 *                 para obter um novo par de tokens
 * - tipo:         sempre "Bearer" — indica como enviar o token:
 *                 Authorization: Bearer <token>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;

    private String refreshToken;

    @Builder.Default
    private String tipo = "Bearer";

    private String email;
    private String nome;
    private String role;
}
