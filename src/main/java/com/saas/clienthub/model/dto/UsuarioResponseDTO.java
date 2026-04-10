package com.saas.clienthub.model.dto;

import com.saas.clienthub.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de saída para exibir dados do Usuário.
 *
 * Note que a senha NUNCA aparece no DTO de resposta — é uma informação
 * sensível que não deve ser exposta em APIs ou templates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private Role role;
    private Long empresaId;
    private String empresaNome;
    private Boolean ativo;
    private LocalDateTime dataCadastro;
}
