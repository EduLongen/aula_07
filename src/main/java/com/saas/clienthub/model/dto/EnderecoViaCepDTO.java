package com.saas.clienthub.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnderecoViaCepDTO {

    private String cep;
    private String logradouro;
    private String complemento;
    private String bairro;

    @JsonProperty("localidade")
    private String localidade;

    private String uf;
    private Boolean erro;
}
