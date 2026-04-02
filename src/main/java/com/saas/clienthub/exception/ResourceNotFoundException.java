package com.saas.clienthub.exception;

/**
 * Exceção lançada quando um recurso não é encontrado no banco de dados.
 * Mapeada para HTTP 404 Not Found pelo GlobalExceptionHandler.
 *
 * =====================================================================
 * CONCEITO: Exceções de Domínio (Domain Exceptions)
 * =====================================================================
 * Em vez de usar exceções genéricas (como RuntimeException ou IllegalArgumentException),
 * criamos exceções com nomes que descrevem o problema de negócio.
 * Isso torna o código mais legível e o tratamento de erros mais preciso.
 *
 * Extendemos RuntimeException (unchecked exception) para não precisar
 * declarar throws em cada método que a lança — o Spring gerencia isso.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message Mensagem descritiva do que não foi encontrado.
     *                Ex: "Empresa não encontrada com id: 5"
     *                Ex: "Cliente não encontrado com id: 3"
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
