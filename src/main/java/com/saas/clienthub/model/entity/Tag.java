package com.saas.clienthub.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade JPA que representa uma Tag (rótulo) associada a Clientes dentro de uma Empresa.
 *
 * =====================================================================
 * CONCEITO: Isolamento Multi-Tenant em Tags
 * =====================================================================
 * Cada Tag pertence a UMA empresa. A combinação (empresa_id, nome) é única —
 * empresas diferentes podem ter tags com o mesmo nome, mas uma empresa não
 * pode ter duas tags com o mesmo nome.
 *
 * =====================================================================
 * CONCEITO: Índice Composto Único
 * =====================================================================
 * @Table(indexes = @Index(columnList = "empresa_id, nome", unique = true))
 * cria um índice UNIQUE composto no banco. Isso:
 *   1. Acelera buscas por (empresa_id, nome)
 *   2. Garante unicidade a nível de banco (mesmo sem passar pela validação Java)
 *
 * =====================================================================
 * CONCEITO: Relacionamento Tag ↔ Cliente
 * =====================================================================
 * Tag NÃO tem referência a Cliente. O ManyToMany é UNIDIRECIONAL —
 * apenas Cliente conhece suas tags, via tabela de junção "cliente_tags".
 * Vantagem: entidade mais simples, sem precisar sincronizar os dois lados.
 */
@Entity
@Table(
        name = "tags",
        indexes = @Index(name = "idx_tag_empresa_nome", columnList = "empresa_id, nome", unique = true)
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String nome;

    /** Cor hex para o badge visual: #RRGGBB (ex: "#007AFF") */
    @Size(max = 7)
    @Column(length = 7)
    private String cor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @ToString.Exclude
    private Empresa empresa;

    @Builder.Default
    private Boolean ativo = true;

    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
    }
}
