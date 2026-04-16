package com.saas.clienthub.service;

import com.saas.clienthub.exception.BusinessException;
import com.saas.clienthub.model.dto.LoginResponseDTO;
import com.saas.clienthub.model.entity.RefreshToken;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.RefreshTokenRepository;
import com.saas.clienthub.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service que gerencia o ciclo de vida dos refresh tokens.
 *
 * =====================================================================
 * RESPONSABILIDADES
 * =====================================================================
 * 1. Criar novos refresh tokens no login
 * 2. Renovar o access token via rotação (revoga o refresh antigo,
 *    emite um novo refresh + um novo access token)
 * 3. Revogar refresh tokens (logout)
 * 4. Detectar reuso de tokens revogados (indicativo de roubo)
 * 5. Limpar tokens expirados do banco (chamado pelo job agendado)
 *
 * =====================================================================
 * CONCEITO: Rotação de Refresh Tokens
 * =====================================================================
 * A cada uso do refresh token, ele é revogado e substituído por um novo:
 *
 *   Login     → emite Token A (access + refresh A)
 *   Refresh   → revoga A, emite Token B (access + refresh B)
 *   Refresh   → revoga B, emite Token C (access + refresh C)
 *   ...
 *
 * Isso reduz a janela de exposição: se um atacante roubar o refresh
 * token, ele só consegue usar uma única vez antes do token legítimo
 * ser rotacionado (e vice-versa).
 *
 * =====================================================================
 * CONCEITO: Detecção de Reuso
 * =====================================================================
 * Se um refresh token já revogado for reapresentado, isso significa:
 *   - o usuário legítimo já o rotacionou (então não poderia estar usando
 *     o token antigo de novo)
 *   - OU um atacante conseguiu o token e tentou reutilizá-lo
 *
 * Como não dá para distinguir os dois cenários, assumimos o pior caso
 * e revogamos TODOS os refresh tokens ativos do usuário, forçando um
 * novo login. O access token JWT continua válido até sua expiração,
 * mas não poderá ser renovado.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final long refreshExpiration;

    /**
     * Injeção via construtor.
     * O valor de jwt.refresh-expiration vem do application.properties
     * (ou da variável de ambiente JWT_REFRESH_EXPIRATION).
     */
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtService jwtService,
                               @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * Cria um novo refresh token para o usuário e persiste no banco.
     *
     * @param usuario o dono do token
     * @return a entidade RefreshToken salva (contém o UUID e a data de expiração)
     */
    @Transactional
    public RefreshToken criarRefreshToken(Usuario usuario) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .usuario(usuario)
                .dataExpiracao(LocalDateTime.now().plus(Duration.ofMillis(refreshExpiration)))
                .revogado(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Renova o par access + refresh token via rotação.
     *
     * Passos:
     *   1. Valida que o refresh token existe no banco
     *   2. Se já foi revogado → detecção de reuso → revoga todos os tokens
     *      do usuário e lança exceção
     *   3. Se expirou → lança exceção (o cliente deve refazer login)
     *   4. Revoga o refresh token atual, emite um novo e liga os dois
     *      via tokenSubstituto (para rastreamento da cadeia de rotação)
     *   5. Gera um novo access token JWT
     *   6. Retorna LoginResponseDTO com o novo par de tokens
     *
     * A anotação @Transactional garante que revogação do antigo +
     * criação do novo são atômicas. Se algo falhar no meio, nenhuma
     * alteração é persistida (o usuário não fica "preso" sem token).
     *
     * @param refreshTokenString o UUID do refresh token a renovar
     * @return novo LoginResponseDTO com access token + refresh token atualizados
     * @throws BusinessException se o token for inválido, revogado ou expirado
     */
    @Transactional
    public LoginResponseDTO renovarToken(String refreshTokenString) {
        // 1. Busca o token no banco
        RefreshToken tokenAtual = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));

        Usuario usuario = tokenAtual.getUsuario();

        // 2. Detecção de reuso: token já foi revogado, alguém está tentando usar de novo
        if (Boolean.TRUE.equals(tokenAtual.getRevogado())) {
            log.warn("REUSO DETECTADO: refresh token revogado foi reapresentado. Usuario: {}. Revogando todos os tokens.",
                    usuario.getEmail());
            revogarTodosTokensDoUsuario(usuario);
            throw new BusinessException(
                    "Refresh token reutilizado — todos os tokens foram revogados por segurança. Faça login novamente."
            );
        }

        // 3. Expiração: token ainda ativo, mas já passou da data
        if (tokenAtual.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expirado — faça login novamente");
        }

        // 4. Rotação: cria o novo refresh token ANTES de revogar o atual,
        // para poder preencher o campo tokenSubstituto
        RefreshToken novoRefreshToken = criarRefreshToken(usuario);

        tokenAtual.setRevogado(true);
        tokenAtual.setTokenSubstituto(novoRefreshToken.getToken());
        refreshTokenRepository.save(tokenAtual);

        // 5. Gera novo access token JWT
        String novoAccessToken = jwtService.gerarToken(
                usuario.getEmail(),
                usuario.getRole().name()
        );

        // 6. Monta a resposta no mesmo formato do login
        return LoginResponseDTO.builder()
                .token(novoAccessToken)
                .refreshToken(novoRefreshToken.getToken())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .role(usuario.getRole().name())
                .build();
    }

    /**
     * Revoga um refresh token específico (operação de logout).
     * O access token JWT continua válido até expirar, mas não poderá ser renovado.
     *
     * @param refreshTokenString o UUID do token a revogar
     * @throws BusinessException se o token não for encontrado
     */
    @Transactional
    public void revogarToken(String refreshTokenString) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));

        token.setRevogado(true);
        refreshTokenRepository.save(token);
    }

    /**
     * Revoga em massa todos os refresh tokens ativos de um usuário.
     * Usado pela detecção de reuso como medida de segurança.
     *
     * Privado porque só faz sentido ser chamado internamente pela
     * lógica de detecção de reuso.
     */
    private void revogarTodosTokensDoUsuario(Usuario usuario) {
        List<RefreshToken> tokensAtivos = refreshTokenRepository.findByUsuarioAndRevogadoFalse(usuario);
        tokensAtivos.forEach(t -> t.setRevogado(true));
        refreshTokenRepository.saveAll(tokensAtivos);
    }

    /**
     * Remove do banco todos os refresh tokens com data de expiração no passado.
     * Chamado periodicamente pelo RefreshTokenCleanupService.
     *
     * @return número de registros deletados
     */
    @Transactional
    public int limparTokensExpirados() {
        int removidos = refreshTokenRepository.deleteByDataExpiracaoBefore(LocalDateTime.now());
        if (removidos > 0) {
            log.info("Limpeza de refresh tokens expirados: {} registros removidos", removidos);
        }
        return removidos;
    }
}
