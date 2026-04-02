package com.saas.clienthub.exception;

import com.saas.clienthub.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tratamento global de exceções para toda a camada REST da aplicação.
 *
 * =====================================================================
 * CONCEITO: @RestControllerAdvice
 * =====================================================================
 * Intercepta exceções lançadas em QUALQUER @RestController da aplicação.
 * Sem ele, uma exceção não tratada retornaria uma página de erro HTML
 * ou um JSON genérico do Spring. Com ele, controlamos exatamente qual
 * HTTP status e mensagem o cliente recebe.
 *
 * É a implementação do padrão "Cross-Cutting Concern" (preocupação transversal):
 * o tratamento de erros fica em um único lugar em vez de estar espalhado
 * em todos os controllers.
 *
 * =====================================================================
 * CONCEITO: Códigos HTTP de Erro
 * =====================================================================
 * 400 Bad Request        → dados inválidos (falha de validação Bean Validation)
 * 404 Not Found          → recurso não encontrado no banco
 * 422 Unprocessable      → dados válidos mas regra de negócio violada (ex: CNPJ duplicado)
 * 500 Internal Server Error → erro inesperado no servidor
 *
 * =====================================================================
 * CONCEITO: @ExceptionHandler
 * =====================================================================
 * Cada método anotado com @ExceptionHandler trata um tipo específico de exceção.
 * O Spring roteia a exceção para o método correto automaticamente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata ResourceNotFoundException → HTTP 404 Not Found.
     * Lançada quando um registro buscado no banco não é encontrado.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value()) // 404
                .message(ex.getMessage())             // mensagem descritiva (ex: "Empresa não encontrada com id: 5")
                .timestamp(LocalDateTime.now())        // momento do erro
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Trata BusinessException → HTTP 422 Unprocessable Entity.
     * Lançada quando os dados são tecnicamente válidos, mas violam
     * uma regra de negócio (ex: CNPJ já cadastrado, email duplicado).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(422)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(422).body(error);
    }

    /**
     * Trata MethodArgumentNotValidException → HTTP 400 Bad Request.
     * Lançada automaticamente pelo Spring quando @Valid falha em um @RequestBody.
     * Extrai a lista de todos os erros de campo (ex: "nome: Nome é obrigatório").
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Percorre todos os erros de campo e formata como "campo: mensagem"
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value()) // 400
                .message("Erro de validação")
                .timestamp(LocalDateTime.now())
                .errors(errors) // lista detalhada dos campos inválidos
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Trata qualquer outra exceção não prevista → HTTP 500 Internal Server Error.
     * Mensagem genérica para não expor detalhes internos ao cliente.
     * O stack trace completo aparece nos logs do servidor para diagnóstico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
                .message("Erro interno do servidor")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
