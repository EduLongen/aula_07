package com.saas.clienthub.repository;

import com.saas.clienthub.model.entity.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de acesso ao banco de dados para a entidade Empresa.
 *
 * =====================================================================
 * CONCEITO: Spring Data JPA — Repository
 * =====================================================================
 * Ao estender JpaRepository<Empresa, Long>, ganhamos AUTOMATICAMENTE:
 *   - save(empresa)          → INSERT ou UPDATE
 *   - findById(id)           → SELECT por PK
 *   - findAll()              → SELECT todos
 *   - delete(empresa)        → DELETE
 *   - count()                → COUNT(*)
 *   - existsById(id)         → EXISTS
 *   ... e muito mais
 *
 * O Spring cria a implementação em tempo de execução — não precisamos
 * escrever nenhuma classe concreta ou SQL para os métodos básicos.
 *
 * =====================================================================
 * CONCEITO: Query Methods (Derived Queries)
 * =====================================================================
 * O Spring Data interpreta o nome do método e gera a SQL automaticamente.
 * Exemplo:
 *   findByAtivaTrue()
 *   → SELECT * FROM empresas WHERE ativa = true
 *
 *   findByCnpj(String cnpj)
 *   → SELECT * FROM empresas WHERE cnpj = ?
 *
 *   existsByCnpj(String cnpj)
 *   → SELECT COUNT(*) > 0 FROM empresas WHERE cnpj = ?
 *
 *   countByAtivaTrue()
 *   → SELECT COUNT(*) FROM empresas WHERE ativa = true
 *
 * Palavras-chave disponíveis: findBy, existsBy, countBy, deleteBy,
 * And, Or, True, False, Containing, IgnoreCase, OrderBy, etc.
 */
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /** Retorna apenas empresas ativas (ativa = true) */
    List<Empresa> findByAtivaTrue();

    /** Paginação — lista todas empresas ordenadas e paginadas */
    Page<Empresa> findAll(Pageable pageable);

    /** Paginação — apenas empresas ativas */
    Page<Empresa> findByAtivaTrue(Pageable pageable);

    /**
     * Busca empresa pelo CNPJ.
     * Retorna Optional<Empresa> — nunca nulo, pode estar vazio.
     * Força o código chamador a tratar o caso "não encontrado".
     */
    Optional<Empresa> findByCnpj(String cnpj);

    /**
     * Verifica se já existe empresa com este CNPJ.
     * Mais eficiente que findByCnpj() quando só precisamos saber se existe —
     * o banco executa EXISTS em vez de trazer o registro completo.
     */
    boolean existsByCnpj(String cnpj);

    /** Conta quantas empresas estão ativas — usado no Dashboard */
    long countByAtivaTrue();
}
