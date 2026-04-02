package com.saas.clienthub.model.dto;

import com.saas.clienthub.model.entity.Plano;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaResponseDTO {

    private Long id;
    private String nome;
    private String cnpj;
    private String email;
    private Plano plano;
    private Boolean ativa;
    private LocalDateTime dataCadastro;
    private Long totalClientes;
}
