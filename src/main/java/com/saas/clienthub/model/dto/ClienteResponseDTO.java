package com.saas.clienthub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private String cep;
    private String logradouro;
    private String bairro;
    private String cidade;
    private String uf;
    private Boolean ativo;
    private LocalDateTime dataCadastro;
    private Long empresaId;
    private String empresaNome;

    /** Tags associadas a este cliente — inclui nome e cor para renderizar badges no UI */
    @Builder.Default
    private List<TagResponseDTO> tags = new ArrayList<>();
}
