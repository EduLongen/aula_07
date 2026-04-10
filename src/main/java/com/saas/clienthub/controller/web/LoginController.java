package com.saas.clienthub.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller para as páginas de login e acesso negado.
 *
 * =====================================================================
 * CONCEITO: Página de Login Customizada
 * =====================================================================
 * Por padrão, o Spring Security gera uma página de login genérica.
 * Ao configurar .loginPage("/login") no SecurityConfig, dizemos ao
 * Spring para usar a NOSSA página. Mas precisamos criar este controller
 * para servir o template login.html.
 *
 * O formulário de login deve:
 * - Enviar POST para /login (processado pelo Spring Security automaticamente)
 * - Ter campos name="username" (email) e name="password" (senha)
 * - Incluir o token CSRF (Thymeleaf adiciona automaticamente com th:action)
 *
 * =====================================================================
 * CONCEITO: Redirect se já autenticado
 * =====================================================================
 * Se o usuário já está logado e acessa /login, não faz sentido mostrar
 * o formulário novamente. Verificamos o Authentication e redirecionamos
 * para a página inicial.
 */
@Controller
public class LoginController {

    /**
     * GET /login → exibe a página de login.
     * Se o usuário já estiver autenticado, redireciona para o dashboard.
     */
    @GetMapping("/login")
    public String login(Authentication authentication) {
        // Se já está logado → redireciona para a home
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "login"; // → templates/login.html
    }

    /**
     * GET /acesso-negado → exibe a página de erro 403 (Forbidden).
     * O SecurityConfig configura esta URL em .accessDeniedPage("/acesso-negado").
     */
    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado"; // → templates/acesso-negado.html
    }
}
