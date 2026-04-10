package com.saas.clienthub.config;

import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler customizado executado APÓS o login ser bem-sucedido.
 *
 * =====================================================================
 * CONCEITO: AuthenticationSuccessHandler
 * =====================================================================
 * Por padrão, o Spring Security redireciona todo mundo para "/" após o login.
 * Com um handler customizado, podemos redirecionar cada role para uma
 * página diferente:
 *
 * - ADMIN       → / (dashboard global com todas as empresas)
 * - GESTOR      → /empresas/{empresaId} (detalhes da sua empresa)
 * - USUARIO     → /empresas/{empresaId} (detalhes da sua empresa)
 *
 * Isso melhora a experiência do usuário — ele já cai direto na tela
 * relevante para o seu papel no sistema.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;

    public CustomAuthenticationSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Chamado automaticamente pelo Spring Security quando o login é bem-sucedido.
     *
     * @param request        a requisição HTTP do login
     * @param response       a resposta HTTP (usada para redirecionar)
     * @param authentication contém os dados do usuário autenticado (email, roles)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // Verifica se o usuário tem a role ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            // ADMIN → dashboard global
            response.sendRedirect("/");
        } else {
            // GESTOR/USUARIO → busca a empresa do usuário e redireciona
            String email = authentication.getName(); // email do usuário logado
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

            if (usuario != null && usuario.getEmpresa() != null) {
                // Redireciona para a página de detalhes da empresa do usuário
                response.sendRedirect("/empresas/" + usuario.getEmpresa().getId());
            } else {
                // Fallback: se não encontrar a empresa, vai para o dashboard
                response.sendRedirect("/");
            }
        }
    }
}
