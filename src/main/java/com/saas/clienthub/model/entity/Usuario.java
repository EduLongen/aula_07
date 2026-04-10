package com.saas.clienthub.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade JPA que representa um Usuário do sistema.
 *
 * =====================================================================
 * CONCEITO: Autenticação vs Autorização
 * =====================================================================
 * - Autenticação: "Quem é você?" → email + senha → Spring Security valida
 * - Autorização:  "O que você pode fazer?" → Role (ADMIN, GESTOR, USUARIO)
 *
 * O campo "email" é usado como username no login (Spring Security).
 * O campo "senha" é armazenado como hash BCrypt — NUNCA em texto puro.
 *
 * =====================================================================
 * CONCEITO: BCrypt
 * =====================================================================
 * BCrypt é um algoritmo de hashing para senhas que inclui um hash
 * aleatório automaticamente. Isso significa que a mesma senha gera
 * hashes diferentes a cada vez, protegendo contra ataques de rainbow table.
 *
 * =====================================================================
 * CONCEITO: Soft Delete
 * =====================================================================
 * Em vez de deletar o usuário do banco (DELETE), marcamos "ativo = false".
 * Isso preserva o histórico e permite reativação futura.
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    /** Chave primária — gerada automaticamente pelo banco (IDENTITY = auto_increment) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome completo do usuário — exibido na navbar e nas listagens */
    @NotBlank
    @Size(max = 100)
    private String nome;

    /**
     * Email do usuário — usado como "username" no login do Spring Security.
     * unique = true → o banco impede dois usuários com o mesmo email.
     */
    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    /**
     * Senha armazenada como hash BCrypt.
     * NUNCA armazenamos senhas em texto puro — é uma vulnerabilidade crítica.
     * O BCryptPasswordEncoder gera o hash antes de salvar no banco.
     */
    @NotBlank
    private String senha;

    /**
     * Papel do usuário no sistema — define o que ele pode acessar.
     * @Enumerated(STRING) → salva como texto no banco ("ADMIN", "GESTOR", "USUARIO")
     */
    @Enumerated(EnumType.STRING)
    @NotNull
    private Role role;

    /**
     * Empresa à qual o usuário pertence.
     *
     * - ADMIN: empresa é null (acessa todas as empresas)
     * - GESTOR/USUARIO: empresa é obrigatória (acessa apenas dados da sua empresa)
     *
     * fetch = LAZY → a Empresa só é carregada do banco quando acessarmos o campo
     * nullable = true → permite null para administradores globais
     *
     * @ToString.Exclude → evita loop infinito no toString() (Empresa → Usuarios → Empresa...)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    @ToString.Exclude
    private Empresa empresa;

    /** Soft delete: false = desativado (não consegue logar), true = ativo */
    @Builder.Default
    private Boolean ativo = true;

    /** Data de criação — preenchida automaticamente pelo @PrePersist */
    private LocalDateTime dataCadastro;

    /** Data da última atualização — preenchida automaticamente pelo @PreUpdate */
    private LocalDateTime dataAtualizacao;

    /** Callback JPA: executado automaticamente ANTES do primeiro INSERT */
    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
    }

    /** Callback JPA: executado automaticamente ANTES de cada UPDATE */
    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}
