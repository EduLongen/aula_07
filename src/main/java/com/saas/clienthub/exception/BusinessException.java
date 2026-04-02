package com.saas.clienthub.exception;

/**
 * Exceção lançada quando uma operação viola uma regra de negócio.
 * Mapeada para HTTP 422 Unprocessable Entity pelo GlobalExceptionHandler.
 *
 * =====================================================================
 * CONCEITO: Diferença entre 400 e 422
 * =====================================================================
 * 400 Bad Request → os dados em si são inválidos (campo obrigatório vazio,
 *                   formato errado). Detectado pelo Bean Validation.
 *
 * 422 Unprocessable → os dados são tecnicamente válidos, mas a operação
 *                     não pode ser realizada por uma regra de negócio.
 *                     Ex: CNPJ "11222333000181" é um CNPJ válido,
 *                     mas já existe no banco — regra de unicidade violada.
 *
 * =====================================================================
 * CONCEITO: Quando usar BusinessException?
 * =====================================================================
 * - CNPJ já cadastrado
 * - Email duplicado no mesmo tenant
 * - CEP não encontrado (retorno de API externa)
 * - Qualquer validação que depende do estado atual do banco
 */
public class BusinessException extends RuntimeException {

    /**
     * @param message Mensagem descritiva da regra violada.
     *                Ex: "CNPJ já cadastrado: 11222333000181"
     *                Ex: "Email já cadastrado nesta empresa: joao@email.com"
     */
    public BusinessException(String message) {
        super(message);
    }
}
