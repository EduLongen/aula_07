package com.saas.clienthub.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade JPA que representa um Cliente dentro de uma Empresa (tenant).
 *
 * =====================================================================
 * CONCEITO: Isolamento Multi-Tenant
 * =====================================================================
 * Todo cliente pertence a uma empresa. NUNCA buscamos um cliente só pelo ID —
 * sempre usamos ID + empresaId nas queries. Isso garante que uma empresa
 * jamais acesse dados de outra empresa (isolamento de tenant).
 *
 * =====================================================================
 * CONCEITO: Relacionamento @ManyToOne
 * =====================================================================
 * Muitos Clientes pertencem a Uma Empresa.
 * O lado @ManyToOne é o "dono" da relação — é aqui que fica a coluna
 * de chave estrangeira (empresa_id) na tabela clientes.
 *
 * =====================================================================
 * CONCEITO: FetchType.LAZY
 * =====================================================================
 * Por padrão, @ManyToOne carrega a entidade relacionada imediatamente
 * (EAGER). Com LAZY, a Empresa só é buscada do banco quando acessarmos
 * o campo empresa.getXxx() — economizando queries desnecessárias.
 */
@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    /** Chave primária — gerada automaticamente pelo banco */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String nome;

    @Email
    @NotBlank
    private String email;

    /** Campo opcional — não tem anotação @NotBlank */
    private String telefone;

    /** Apenas os dígitos do CEP (8 caracteres) */
    @Size(max = 9)
    private String cep;

    /**
     * Campos de endereço — preenchidos automaticamente
     * pelo ViaCepService quando o cliente é salvo com um CEP válido.
     */
    private String logradouro;
    private String bairro;
    private String cidade;

    @Size(max = 2)
    private String uf;

    /** Soft delete: falso = desativado, verdadeiro = ativo */
    @Builder.Default
    private Boolean ativo = true;

    /** Datas de auditoria preenchidas pelos callbacks JPA */
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;

    /**
     * Chave estrangeira para a tabela empresas.
     *
     * fetch = LAZY    → a Empresa NÃO é carregada do banco automaticamente,
     *                   só quando precisarmos acessar seus dados
     * nullable = false → empresa_id não pode ser nulo — todo cliente
     *                    DEVE pertencer a uma empresa
     *
     * @ToString.Exclude → evita loop infinito: Cliente → Empresa → List<Cliente> → Cliente...
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @ToString.Exclude
    private Empresa empresa;

    /** Executado automaticamente pelo JPA antes do primeiro INSERT */
    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
    }

    /** Executado automaticamente pelo JPA antes de cada UPDATE */
    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}
