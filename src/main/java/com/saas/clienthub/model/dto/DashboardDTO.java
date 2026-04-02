package com.saas.clienthub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    private long totalEmpresas;
    private long totalClientes;
    private long clientesAtivos;
    private long empresasAtivas;
}
