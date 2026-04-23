package com.saas.clienthub.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para criação/edição de Tag.
 *
 * CONCEITO: @Pattern valida string com regex.
 * Aqui garantimos que a cor seja um hex color válido (#RRGGBB).
 * O campo é opcional — se não informado, o service usa "#007AFF" como padrão.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagRequestDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 50, message = "Nome deve ter no máximo 50 caracteres")
    private String nome;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato #RRGGBB (ex: #007AFF)")
    private String cor;
}
