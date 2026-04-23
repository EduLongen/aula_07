package com.saas.clienthub.repository;

import com.saas.clienthub.model.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de acesso ao banco de dados para a entidade Cliente.
 *
 * =====================================================================
 * CONCEITO: Isolamento Multi-Tenant nos Repositórios
 * =====================================================================
 * Repare que quase todos os métodos recebem empresaId como parâmetro.
 * Isso garante que uma empresa NUNCA acesse clientes de outra empresa.
 *
 * =====================================================================
 * CONCEITO: Paginação com Spring Data (Pageable / Page)
 * =====================================================================
 * Em vez de retornar List<Cliente>, retornamos Page<Cliente>, que contém:
 *   - conteúdo da página atual
 *   - número total de elementos e páginas
 *   - número da página atual
 *
 * Isso é crucial em produção — listar 100.000 clientes em memória quebraria
 * a aplicação. Pageable define page, size e sort.
 *
 * =====================================================================
 * CONCEITO: JOIN FETCH (resolvendo N+1)
 * =====================================================================
 * Com LAZY, acessar cliente.getTags() dispara UMA query adicional para
 * cada cliente → se listarmos 100 clientes, são 101 queries (N+1).
 *
 * @Query com JOIN FETCH traz tudo em UMA única query:
 *   SELECT c.*, t.* FROM clientes c
 *   LEFT JOIN cliente_tags ct ON ct.cliente_id = c.id
 *   LEFT JOIN tags t ON t.id = ct.tag_id
 *   WHERE c.empresa_id = ?
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Todos os clientes de uma empresa (ativos e inativos) — sem paginação */
    List<Cliente> findByEmpresaId(Long empresaId);

    /** Apenas os clientes ativos de uma empresa */
    List<Cliente> findByEmpresaIdAndAtivoTrue(Long empresaId);

    /** Paginação — lista clientes da empresa com ordenação e paginação */
    Page<Cliente> findByEmpresaId(Long empresaId, Pageable pageable);

    /** Paginação — apenas ativos */
    Page<Cliente> findByEmpresaIdAndAtivoTrue(Long empresaId, Pageable pageable);

    /** Pesquisa paginada por nome */
    Page<Cliente> findByEmpresaIdAndNomeContainingIgnoreCase(Long empresaId, String nome, Pageable pageable);

    /**
     * Busca um cliente garantindo que pertence à empresa informada.
     * findByIdAndEmpresaId → WHERE id = ? AND empresa_id = ?
     */
    Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId);

    /**
     * Verifica se já existe um cliente com este email nesta empresa.
     * Um mesmo email pode existir em empresas diferentes (tenants distintos),
     * mas não pode se repetir dentro da mesma empresa.
     */
    Optional<Cliente> findByEmailAndEmpresaId(String email, Long empresaId);

    /** Conta total de clientes de uma empresa — exibido na tela de detalhes */
    long countByEmpresaId(Long empresaId);

    /** Conta total de clientes ativos em todas as empresas — usado no Dashboard global (ADMIN) */
    long countByAtivoTrue();

    /** Conta clientes ativos de uma empresa específica — usado no Dashboard do tenant */
    long countByEmpresaIdAndAtivoTrue(Long empresaId);

    /** Pesquisa clientes por nome dentro de uma empresa — versão sem paginação */
    List<Cliente> findByEmpresaIdAndNomeContainingIgnoreCase(Long empresaId, String nome);

    /**
     * Busca com JOIN FETCH para carregar empresa + tags em uma única query.
     * Evita o problema de N+1 queries ao listar clientes com suas tags.
     */
    @Query("SELECT DISTINCT c FROM Cliente c " +
           "JOIN FETCH c.empresa " +
           "LEFT JOIN FETCH c.tags " +
           "WHERE c.empresa.id = :empresaId")
    List<Cliente> findByEmpresaIdWithTags(@Param("empresaId") Long empresaId);

    /** Busca um cliente específico já trazendo empresa + tags em uma query */
    @Query("SELECT c FROM Cliente c " +
           "JOIN FETCH c.empresa " +
           "LEFT JOIN FETCH c.tags " +
           "WHERE c.id = :id AND c.empresa.id = :empresaId")
    Optional<Cliente> findByIdAndEmpresaIdWithTags(@Param("id") Long id, @Param("empresaId") Long empresaId);
}
