package com.saas.clienthub.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para a requisição de renovação de token (POST /api/auth/refresh)
 * e de logout (POST /api/auth/logout).
 *
 * Recebe o refresh token em JSON:
 * {
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
 * }
 *
 * O mesmo DTO serve para os dois endpoints porque ambos precisam apenas
 * do refresh token para operar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Refresh token é obrigatório")
    private String refreshToken;
}
