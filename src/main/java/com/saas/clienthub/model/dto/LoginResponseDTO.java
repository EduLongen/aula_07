package com.saas.clienthub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta do login na API REST (JWT).
 *
 * Retorna o token JWT e informações básicas do usuário:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "tipo": "Bearer",
 *   "email": "admin@clienthub.com",
 *   "nome": "Administrador",
 *   "role": "ADMIN"
 * }
 *
 * O campo "tipo" é sempre "Bearer" — indica ao cliente como
 * enviar o token no header: Authorization: Bearer <token>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;

    @Builder.Default
    private String tipo = "Bearer";

    private String email;
    private String nome;
    private String role;
}
