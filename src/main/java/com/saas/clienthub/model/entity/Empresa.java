package com.saas.clienthub.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade JPA que representa uma Empresa (tenant) no sistema multi-tenant.
 *
 * =====================================================================
 * CONCEITO: O que é uma Entidade JPA?
 * =====================================================================
 * JPA (Jakarta Persistence API) é uma especificação Java para mapeamento
 * objeto-relacional (ORM). Em vez de escrever SQL manualmente, anotamos
 * a classe e o Hibernate (implementação do JPA) gera as queries automaticamente.
 *
 * @Entity  → diz ao JPA que esta classe representa uma tabela no banco
 * @Table   → define o nome exato da tabela (se omitido, usa o nome da classe)
 *
 * =====================================================================
 * CONCEITO: Lombok
 * =====================================================================
 * Lombok é uma biblioteca que gera código repetitivo automaticamente
 * em tempo de compilação, via anotações:
 *
 * @Data            → gera getters, setters, toString, equals e hashCode
 * @NoArgsConstructor → gera construtor sem argumentos (exigido pelo JPA)
 * @AllArgsConstructor → gera construtor com todos os campos
 * @Builder         → permite criar objetos com o padrão Builder:
 *                    Empresa.builder().nome("X").email("y@z").build()
 *
 * =====================================================================
 * CONCEITO: Multi-tenancy
 * =====================================================================
 * Cada Empresa é um "tenant" — um cliente isolado da plataforma SaaS.
 * Todos os Clientes pertencem a uma Empresa, garantindo isolamento de dados.
 */
@Entity
@Table(name = "empresas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    /**
     * Chave primária da tabela.
     * @Id              → marca este campo como chave primária
     * @GeneratedValue  → o banco gera o valor automaticamente
     * IDENTITY         → usa auto_increment/serial do banco (PostgreSQL usa SERIAL)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @NotBlank → valida que o campo não é nulo, vazio ou só espaços (Bean Validation)
     * @Size     → valida o tamanho máximo
     * O Hibernate traduz @Size para varchar(100) no DDL gerado
     */
    @NotBlank
    @Size(max = 100)
    private String nome;

    /**
     * @Column(unique = true) → cria uma constraint UNIQUE no banco.
     * O Hibernate gera: ALTER TABLE empresas ADD CONSTRAINT ... UNIQUE (cnpj)
     * Isso impede dois registros com o mesmo CNPJ diretamente no banco de dados.
     */
    @NotBlank
    @Column(unique = true)
    private String cnpj;

    @Email
    @NotBlank
    private String email;

    /**
     * @Enumerated(EnumType.STRING) → salva o nome do enum como texto no banco.
     * Sem isso, o padrão seria EnumType.ORDINAL (salvaria 0, 1, 2),
     * que é frágil — se mudar a ordem do enum, os dados ficam errados.
     *
     * @Builder.Default → quando usar o Builder, este campo começa com BASICO.
     * Sem isso, o Lombok Builder ignoraria o valor padrão da declaração.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Plano plano = Plano.BASICO;

    /** Soft delete: em vez de deletar do banco, apenas marcamos como inativa */
    @Builder.Default
    private Boolean ativa = true;

    /**
     * Datas de auditoria — preenchidas automaticamente pelos callbacks do JPA.
     * Não precisamos setar estas datas manualmente no código de negócio.
     */
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;

    /**
     * Relacionamento Um-para-Muitos: uma Empresa tem muitos Clientes.
     *
     * mappedBy = "empresa" → o lado "dono" da relação é a entidade Cliente
     *                        (é lá que fica a FK empresa_id na tabela clientes)
     * cascade = ALL        → operações na Empresa se propagam para os Clientes:
     *                        se salvar a empresa, salva os clientes; se deletar, deleta os clientes
     * orphanRemoval = true → se um Cliente for removido da lista, é deletado do banco
     *
     * @ToString.Exclude / @EqualsAndHashCode.Exclude → evita StackOverflowError.
     * Sem isso, Empresa.toString() chamaria Cliente.toString() que chamaria
     * Empresa.toString() de volta — loop infinito.
     */
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Cliente> clientes = new ArrayList<>();

    /**
     * @PrePersist → callback executado ANTES de um INSERT no banco.
     * O JPA chama este método automaticamente antes de salvar pela primeira vez.
     */
    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
    }

    /**
     * @PreUpdate → callback executado ANTES de um UPDATE no banco.
     * Sempre que salvarmos alterações, a data de atualização é renovada.
     */
    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}
