package com.saas.clienthub.repository;

import com.saas.clienthub.model.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repositório da entidade Tag.
 *
 * =====================================================================
 * CONCEITO: Isolamento Multi-Tenant
 * =====================================================================
 * Todos os métodos recebem empresaId — uma empresa nunca vê tags de outra.
 *
 * =====================================================================
 * CONCEITO: findByIdInAndEmpresaId
 * =====================================================================
 * O "In" gera um WHERE id IN (?, ?, ?) — útil para buscar várias tags
 * de uma vez por IDs. Combinado com empresaId, valida que TODAS as tags
 * pertencem à mesma empresa (anti-IDOR).
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** Todas as tags de uma empresa (ativas e inativas) */
    List<Tag> findByEmpresaId(Long empresaId);

    /** Todas as tags de uma empresa — com paginação */
    Page<Tag> findByEmpresaId(Long empresaId, Pageable pageable);

    /** Apenas tags ativas — usadas nos checkboxes do formulário de cliente */
    List<Tag> findByEmpresaIdAndAtivoTrue(Long empresaId);

    /** Busca várias tags por IDs validando que pertencem à empresa informada */
    List<Tag> findByIdInAndEmpresaId(Set<Long> ids, Long empresaId);

    /** Busca uma tag específica garantindo que pertence à empresa (multi-tenant) */
    Optional<Tag> findByIdAndEmpresaId(Long id, Long empresaId);

    /** Verifica se já existe tag com este nome nesta empresa (validação de duplicidade) */
    boolean existsByNomeAndEmpresaId(String nome, Long empresaId);
}
