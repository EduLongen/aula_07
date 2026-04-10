package com.saas.clienthub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que intercepta requisições à API REST e autentica via JWT.
 *
 * =====================================================================
 * CONCEITO: OncePerRequestFilter
 * =====================================================================
 * OncePerRequestFilter garante que o filtro é executado apenas UMA VEZ
 * por requisição (mesmo que haja redirects internos). É a classe base
 * recomendada pelo Spring para filtros de segurança customizados.
 *
 * =====================================================================
 * CONCEITO: Fluxo de autenticação JWT
 * =====================================================================
 * 1. Cliente envia requisição com header: Authorization: Bearer <token>
 * 2. Este filtro intercepta a requisição ANTES de chegar no controller
 * 3. Extrai o token do header e valida com JwtService
 * 4. Se válido: cria um Authentication e coloca no SecurityContext
 *    → O controller executa normalmente com o usuário autenticado
 * 5. Se inválido ou ausente: não faz nada (deixa o Spring Security decidir)
 *    → Se a rota exigir autenticação, retorna 401/403
 *
 * =====================================================================
 * CONCEITO: SecurityContextHolder
 * =====================================================================
 * É onde o Spring Security armazena quem está autenticado na thread atual.
 * Ao colocar um Authentication no SecurityContext, informamos ao Spring
 * que "este usuário está logado para esta requisição".
 *
 * Isso permite que:
 * - Os services acessem o usuário logado via SecurityContextHolder
 * - As anotações @PreAuthorize / hasRole() funcionem
 * - O Spring Security não bloqueie a requisição
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Aplica este filtro APENAS em requisições para /api/**.
     * Requisições web (Thymeleaf) usam sessão e não precisam deste filtro.
     *
     * shouldNotFilter retorna TRUE para pular o filtro.
     * Retornamos true quando a URL NÃO começa com /api/ — ou seja,
     * este filtro SÓ executa para rotas da API.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extrai o header Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. Verifica se o header existe e começa com "Bearer "
        //    Se não, passa adiante sem autenticar (a rota pode ser pública)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Remove o prefixo "Bearer " para obter apenas o token
        String token = authHeader.substring(7);

        // 4. Valida o token
        if (jwtService.isTokenValido(token)) {
            // 5. Extrai as informações do token
            String email = jwtService.extrairEmail(token);
            String role = jwtService.extrairRole(token);

            // 6. Cria as authorities (permissões) do Spring Security
            //    "ROLE_ADMIN" → hasRole("ADMIN") retorna true
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            // 7. Cria o token de autenticação do Spring Security
            //    Parâmetros: principal (email), credentials (null — já validou), authorities
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            // 8. Coloca no SecurityContext — agora o Spring Security
            //    reconhece este usuário como autenticado para esta requisição
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 9. Continua a cadeia de filtros (chega no controller eventualmente)
        filterChain.doFilter(request, response);
    }
}
