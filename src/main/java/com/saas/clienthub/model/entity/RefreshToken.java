package com.saas.clienthub.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade JPA que representa um Refresh Token — token opaco de longa
 * duração usado para renovar o access token JWT sem precisar refazer login.
 *
 * =====================================================================
 * CONCEITO: Access Token vs Refresh Token
 * =====================================================================
 * - Access Token (JWT): curta duração (ex: 24h), stateless, vai no header
 *   Authorization de cada requisição. Não é armazenado no servidor.
 *
 * - Refresh Token: longa duração (ex: 7 dias), armazenado no banco
 *   (para permitir revogação), usado APENAS para pedir um novo access token.
 *
 * =====================================================================
 * CONCEITO: Por que UUID opaco e não JWT?
 * =====================================================================
 * Um JWT é auto-contido e stateless — o servidor não consegue "revogar"
 * um JWT antes da expiração sem manter uma blacklist. Refresh tokens PRECISAM
 * ser revogáveis (logout, rotação, detecção de roubo), então usamos um UUID
 * aleatório armazenado no banco. Se o registro está marcado como revogado
 * ou foi deletado, o token é inválido.
 *
 * =====================================================================
 * CONCEITO: Rotação de Refresh Tokens
 * =====================================================================
 * Cada vez que o refresh token é usado para renovar o access token, ele é
 * REVOGADO e um novo refresh token é emitido em seu lugar. Isso limita a
 * janela de uso caso o token seja roubado.
 *
 *   Token A → (usado para renovar) → revogado + Token B emitido
 *   Token B → (usado para renovar) → revogado + Token C emitido
 *
 * =====================================================================
 * CONCEITO: Detecção de Reuso (Replay Attack)
 * =====================================================================
 * Se um refresh token REVOGADO for reapresentado, isso indica que alguém
 * roubou o token — porque o usuário legítimo já o rotacionou. Nesse caso,
 * revogamos TODOS os tokens ativos do usuário por segurança, forçando
 * novo login.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /** Chave primária — gerada automaticamente pelo banco (IDENTITY = auto_increment) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID opaco do refresh token.
     * unique = true → garantia de unicidade em nível de banco.
     * nullable = false → o token nunca pode ser nulo.
     */
    @Column(unique = true, nullable = false)
    private String token;

    /**
     * Usuário dono deste refresh token.
     *
     * fetch = LAZY → o Usuario só é carregado quando acessarmos o campo
     * nullable = false → todo refresh token pertence a um usuário
     * @ToString.Exclude → evita loop infinito no toString()
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    /** Data em que este refresh token expira (após isso, não pode mais ser usado) */
    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    /**
     * Flag de revogação. Um token pode ser revogado por três motivos:
     *   1. Foi usado para rotação (um novo token foi emitido no seu lugar)
     *   2. O usuário fez logout explícito
     *   3. Foi detectado reuso e todos os tokens do usuário foram revogados
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean revogado = false;

    /**
     * UUID do refresh token que substituiu este durante a rotação.
     *
     * Quando Token A é rotacionado para Token B, este campo em A armazena o UUID
     * de B. Isso permite rastrear a cadeia de rotação e, em caso de reuso,
     * identificar que o token faz parte de uma cadeia válida que foi comprometida.
     */
    @Column
    private String tokenSubstituto;

    /** Data de criação — preenchida automaticamente pelo @PrePersist */
    private LocalDateTime dataCadastro;

    /** Callback JPA: executado automaticamente ANTES do primeiro INSERT */
    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
    }
}
