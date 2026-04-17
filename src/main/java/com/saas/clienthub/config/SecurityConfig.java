package com.saas.clienthub.config;

import com.saas.clienthub.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração central do Spring Security com DOIS modos de autenticação.
 *
 * =====================================================================
 * CONCEITO: Dual Authentication (Sessão + JWT)
 * =====================================================================
 * O ClientHub tem dois tipos de cliente:
 *
 * 1. Browser (Thymeleaf) → usa SESSÃO (cookie JSESSIONID)
 *    - Usuário faz login no formulário /login
 *    - Spring Security cria sessão no servidor
 *    - Cookie é enviado automaticamente em cada requisição
 *    - Ideal para aplicações web tradicionais
 *
 * 2. API REST (Postman, curl, apps) → usa JWT (token no header)
 *    - Cliente faz POST /api/auth/login com email+senha
 *    - Recebe um token JWT na resposta
 *    - Envia o token em cada requisição: Authorization: Bearer <token>
 *    - Stateless — o servidor não guarda sessão
 *
 * =====================================================================
 * CONCEITO: Múltiplos SecurityFilterChain
 * =====================================================================
 * O Spring Security permite definir VÁRIOS SecurityFilterChain,
 * cada um com suas próprias regras. O @Order define a prioridade:
 *   - @Order(1) → processado PRIMEIRO (API)
 *   - @Order(2) → processado DEPOIS (Web)
 *
 * A regra securityMatcher() define QUAIS URLs cada chain processa:
 *   - apiFilterChain: "/api/**"
 *   - webFilterChain: tudo o resto
 *
 * Isso permite configurações completamente diferentes:
 *   - API: stateless, sem CSRF, sem form login, com JWT filter
 *   - Web: stateful, com CSRF, com form login, com sessão
 *
 * =====================================================================
 * CONCEITO: PasswordEncoder (BCrypt)
 * =====================================================================
 * BCrypt é o algoritmo recomendado para hash de senhas:
 * - Inclui um "salt" aleatório automaticamente
 * - É propositalmente lento para dificultar ataques de força bruta
 * - A mesma senha gera hashes diferentes (por causa do salt)
 *
 * NUNCA armazene senhas em texto puro ou com algoritmos fracos (MD5, SHA1).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.successHandler = successHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // =========================================================================
    // CHAIN 1: API REST — autenticação via JWT (stateless)
    // =========================================================================

    /**
     * SecurityFilterChain para a API REST (/api/**).
     *
     * @Order(1) → esta chain é avaliada ANTES da chain web.
     * securityMatcher("/api/**") → aplica APENAS em rotas /api/**.
     *
     * Características:
     * - Stateless (sem sessão no servidor — cada requisição é independente)
     * - Sem CSRF (APIs REST não usam cookies de sessão)
     * - Sem form login (autenticação via JWT no header)
     * - JwtAuthenticationFilter inserido antes do filtro padrão
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            // Esta chain só processa URLs /api/**
            .securityMatcher("/api/**")

            // Regras de autorização para a API
            .authorizeHttpRequests(auth -> auth
                // Login JWT é público (precisa estar acessível sem token)
                // /api/cep/** é um BFF público consumido pela UI web (fetch sem Bearer)
                .requestMatchers("/api/auth/**", "/api/cep/**").permitAll()

                // OpenAPI JSON endpoint — precisa estar acessível para o Swagger UI funcionar
                .requestMatchers("/api-docs/**").permitAll()

                // Todas as outras rotas da API exigem autenticação JWT
                .anyRequest().authenticated()
            )

            // Stateless: o servidor NÃO cria sessão para requisições da API.
            // Cada requisição deve trazer seu próprio token JWT.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // CSRF desabilitado para a API.
            // CSRF protege contra ataques baseados em cookies, mas JWT
            // é enviado no header Authorization (não em cookie), então
            // o ataque CSRF não se aplica.
            .csrf(csrf -> csrf.disable())

            // Insere o JwtAuthenticationFilter ANTES do filtro padrão
            // do Spring Security (UsernamePasswordAuthenticationFilter).
            // Assim, quando a requisição chega no filtro padrão,
            // o usuário já está autenticado via JWT.
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =========================================================================
    // CHAIN 2: Web (Thymeleaf) — autenticação via sessão (form login)
    // =========================================================================

    /**
     * SecurityFilterChain para o frontend web (Thymeleaf).
     *
     * @Order(2) → avaliada DEPOIS da chain da API.
     * Sem securityMatcher → processa TODAS as URLs que não foram capturadas pela chain 1.
     *
     * Características:
     * - Stateful (sessão no servidor com cookie JSESSIONID)
     * - CSRF habilitado (Thymeleaf adiciona o token automaticamente nos forms)
     * - Form login customizado (/login)
     * - Logout com invalidação de sessão
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos — sempre liberados (CSS, JS, imagens)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                // Páginas públicas
                .requestMatchers("/login", "/error").permitAll()

                // Swagger UI e OpenAPI JSON — acessíveis sem login
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()

                // Rotas de administração — apenas ADMIN
                .requestMatchers("/admin/**", "/usuarios/**").hasRole("ADMIN")

                // Criação e edição de empresas — apenas ADMIN
                .requestMatchers("/empresas/novo", "/empresas/salvar").hasRole("ADMIN")
                .requestMatchers("/empresas/{id}/editar", "/empresas/{id}/desativar", "/empresas/{id}/ativar").hasRole("ADMIN")

                // Visualização de empresas — qualquer usuário autenticado
                .requestMatchers("/empresas/**").hasAnyRole("ADMIN", "GESTOR", "USUARIO")

                // Qualquer outra URL exige autenticação
                .anyRequest().authenticated()
            )

            // Form login para o frontend web
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Página de acesso negado (403)
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/acesso-negado")
            );

        return http.build();
    }

    /**
     * Bean do PasswordEncoder — usado para criptografar e verificar senhas.
     * Compartilhado entre a autenticação web (form login) e JWT (API).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
