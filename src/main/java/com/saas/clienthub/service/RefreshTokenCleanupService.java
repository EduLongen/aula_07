package com.saas.clienthub.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service agendado que limpa refresh tokens expirados do banco.
 *
 * =====================================================================
 * CONCEITO: @Scheduled
 * =====================================================================
 * Anotação do Spring que marca um método para execução periódica.
 * Requer @EnableScheduling na classe principal (ClientHubApplication).
 *
 * Modos disponíveis:
 *   - fixedRate:         intervalo entre INÍCIOS de execução
 *   - fixedDelay:        intervalo entre o FIM de uma execução e o INÍCIO da próxima
 *   - cron:              expressão cron (ex: "0 0 3 * * *" → todo dia às 3h)
 *   - fixedRateString:   igual ao fixedRate, mas aceita o valor de uma property
 *
 * Usamos fixedRateString para permitir configuração via application.properties
 * (ou variável de ambiente JWT_REFRESH_CLEANUP_INTERVAL).
 *
 * =====================================================================
 * POR QUE LIMPAR?
 * =====================================================================
 * Tokens expirados já são rejeitados pela validação no momento do uso,
 * mas sem uma limpeza periódica a tabela refresh_tokens cresce
 * indefinidamente. Essa rotina mantém a tabela enxuta.
 */
@Service
public class RefreshTokenCleanupService {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanupService(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Remove refresh tokens expirados do banco.
     * Executa a cada jwt.refresh-cleanup-interval milissegundos (padrão: 24h).
     *
     * initialDelay = primeiro ciclo só começa após esse atraso, para não
     * concorrer com o startup da aplicação.
     */
    @Scheduled(
            fixedRateString = "${jwt.refresh-cleanup-interval}",
            initialDelayString = "${jwt.refresh-cleanup-interval}"
    )
    public void executarLimpeza() {
        refreshTokenService.limparTokensExpirados();
    }
}
