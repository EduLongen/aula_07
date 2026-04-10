package com.saas.clienthub.model.dto;

import com.saas.clienthub.model.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para criação/atualização de Usuário.
 *
 * =====================================================================
 * CONCEITO: Por que não usar a Entidade diretamente?
 * =====================================================================
 * O DTO separa o que o cliente envia (formulário/JSON) do que o banco
 * armazena (entidade). Isso permite:
 * - Validar apenas campos do formulário (senha pode ser vazia na edição)
 * - Não expor campos internos (hash da senha, datas de auditoria)
 * - Receber empresaId como Long em vez de um objeto Empresa completo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRequestDTO {

    /** Usado internamente para distinguir criação (null) de edição */
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    private String email;

    /**
     * Senha em texto puro — será criptografada com BCrypt no Service.
     * Na edição, se vier vazia ou null, mantemos a senha atual do banco.
     *
     * Não usamos @NotBlank aqui porque na edição a senha é opcional.
     * A validação de obrigatoriedade na criação é feita no Controller.
     * @Size só valida se o valor não for null — strings vazias passam
     * e são tratadas no Service como "manter senha atual".
     */
    private String senha;

    @NotNull(message = "Role é obrigatória")
    private Role role;

    /** ID da empresa — obrigatório para GESTOR/USUARIO, null para ADMIN */
    private Long empresaId;
}
