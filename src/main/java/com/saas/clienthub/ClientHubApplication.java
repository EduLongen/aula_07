package com.saas.clienthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal da aplicação ClientHub.
 *
 * =====================================================================
 * CONCEITO: @EnableScheduling
 * =====================================================================
 * Habilita o suporte a tarefas agendadas (@Scheduled) no Spring.
 * Necessário para o RefreshTokenCleanupService, que remove do banco
 * os refresh tokens expirados periodicamente.
 */
@SpringBootApplication
@EnableScheduling
public class ClientHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientHubApplication.class, args);
    }

}
