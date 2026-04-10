package com.saas.clienthub.repository;

import com.saas.clienthub.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de acesso ao banco de dados para a entidade Usuario.
 *
 * =====================================================================
 * CONCEITO: Query Methods para Autenticação
 * =====================================================================
 * O método findByEmail é essencial para o Spring Security:
 * quando o usuário faz login, o CustomUserDetailsService chama
 * findByEmail(email) para buscar o registro e verificar a senha.
 *
 * findByEmpresaId é usado para listar os usuários de uma empresa
 * específica (útil para gestão de acessos).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca usuário pelo email (username do login).
     * Retorna Optional — se não encontrar, retorna Optional.empty().
     * Gerado: SELECT * FROM usuarios WHERE email = ?
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Lista todos os usuários de uma empresa específica.
     * Gerado: SELECT * FROM usuarios WHERE empresa_id = ?
     */
    List<Usuario> findByEmpresaId(Long empresaId);

    /**
     * Verifica se já existe um usuário com este email.
     * Usado para validar duplicidade antes de criar um novo usuário.
     * Gerado: SELECT EXISTS(SELECT 1 FROM usuarios WHERE email = ?)
     */
    boolean existsByEmail(String email);
}
