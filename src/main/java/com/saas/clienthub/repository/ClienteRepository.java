package com.saas.clienthub.repository;

import com.saas.clienthub.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

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
 * Exemplo de query gerada:
 *   findByIdAndEmpresaId(1L, 5L)
 *   → SELECT * FROM clientes WHERE id = 1 AND empresa_id = 5
 *
 * Se retornar vazio, significa que o cliente 1 não pertence à empresa 5,
 * e o sistema retorna 404 — protegendo os dados do tenant.
 *
 * =====================================================================
 * CONCEITO: Naming Convention das Query Methods
 * =====================================================================
 * findBy[Campo][Condicao][And/Or][Campo][Condicao]
 *
 * Exemplos:
 *   findByEmpresaId              → WHERE empresa_id = ?
 *   findByEmpresaIdAndAtivoTrue  → WHERE empresa_id = ? AND ativo = true
 *   findByIdAndEmpresaId         → WHERE id = ? AND empresa_id = ?
 *   findByEmailAndEmpresaId      → WHERE email = ? AND empresa_id = ?
 *   NomeContainingIgnoreCase     → WHERE LOWER(nome) LIKE %?%
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Todos os clientes de uma empresa (ativos e inativos) */
    List<Cliente> findByEmpresaId(Long empresaId);

    /** Apenas os clientes ativos de uma empresa */
    List<Cliente> findByEmpresaIdAndAtivoTrue(Long empresaId);

    /**
     * Busca um cliente garantindo que pertence à empresa informada.
     * Principal mecanismo de isolamento multi-tenant na leitura.
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

    /** Conta total de clientes ativos em todas as empresas — usado no Dashboard */
    long countByAtivoTrue();

    /**
     * Pesquisa clientes por nome dentro de uma empresa.
     * Containing → LIKE %nome%
     * IgnoreCase → converte para minúsculas antes de comparar
     *
     * SQL gerada: WHERE empresa_id = ? AND LOWER(nome) LIKE LOWER('%?%')
     */
    List<Cliente> findByEmpresaIdAndNomeContainingIgnoreCase(Long empresaId, String nome);
}
