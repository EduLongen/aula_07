package com.saas.clienthub.repository;

import com.saas.clienthub.model.entity.RefreshToken;
import com.saas.clienthub.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de acesso ao banco de dados para a entidade RefreshToken.
 *
 * =====================================================================
 * CONCEITO: Query Methods para Refresh Tokens
 * =====================================================================
 * O Spring Data JPA cria a implementação automaticamente a partir do
 * nome dos métodos — não precisamos escrever SQL para as operações
 * básicas.
 *
 * findByToken é a consulta mais crítica: toda vez que o cliente pede
 * um novo access token, buscamos o refresh token aqui para validar.
 *
 * findByUsuarioAndRevogadoFalse é usado na detecção de reuso: quando
 * um token revogado é reapresentado, revogamos todos os tokens ainda
 * ativos desse usuário.
 *
 * deleteByDataExpiracaoBefore é chamado pelo job de limpeza agendado,
 * removendo tokens expirados que só ocupam espaço no banco.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca um refresh token pelo seu UUID.
     * Retorna Optional — se não encontrar, retorna Optional.empty().
     * Gerado: SELECT * FROM refresh_tokens WHERE token = ?
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Lista todos os refresh tokens ativos (não-revogados) de um usuário.
     * Usado pela detecção de reuso para revogar em massa.
     * Gerado: SELECT * FROM refresh_tokens WHERE usuario_id = ? AND revogado = false
     */
    List<RefreshToken> findByUsuarioAndRevogadoFalse(Usuario usuario);

    /**
     * Deleta todos os refresh tokens com data de expiração anterior à informada.
     * Executado pelo job agendado de limpeza.
     *
     * @Modifying → obrigatório quando a query altera o banco (DELETE/UPDATE)
     * @Transactional → necessário para operações de modificação em bulk
     *
     * Gerado: DELETE FROM refresh_tokens WHERE data_expiracao < ?
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.dataExpiracao < :dateTime")
    int deleteByDataExpiracaoBefore(LocalDateTime dateTime);
}
