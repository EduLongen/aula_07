package com.saas.clienthub.controller.web;

import com.saas.clienthub.model.entity.Role;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.service.EmpresaService;
import com.saas.clienthub.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller Web responsável pela página inicial (Dashboard).
 *
 * =====================================================================
 * CONCEITO: @Controller (Web) vs @RestController (API)
 * =====================================================================
 * @Controller → os métodos retornam o NOME de um template Thymeleaf.
 *               O Spring localiza o arquivo em /templates/{nome}.html
 *               e o renderiza como HTML para o browser.
 *
 * @RestController → os métodos retornam DADOS (JSON/XML) diretamente.
 *
 * =====================================================================
 * CONCEITO: Model (MVC)
 * =====================================================================
 * Model é o "M" do padrão MVC (Model-View-Controller):
 *   - Controller: recebe a requisição HTTP e prepara os dados
 *   - Model: o mapa de dados passado para o template
 *   - View: o template Thymeleaf que renderiza o HTML
 *
 * model.addAttribute("chave", valor) → disponibiliza ${chave} no template
 *
 * =====================================================================
 * CONCEITO: @ModelAttribute no nível do controller
 * =====================================================================
 * O método anotado com @ModelAttribute é executado ANTES de qualquer
 * método handler desta classe. O valor retornado é automaticamente
 * adicionado ao Model com o nome especificado.
 *
 * Usamos para adicionar "currentPage" a todos os templates do Dashboard,
 * permitindo que a navbar destaque o item ativo corretamente.
 */
@Controller
public class DashboardController {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;

    public DashboardController(EmpresaService empresaService, UsuarioService usuarioService) {
        this.empresaService = empresaService;
        this.usuarioService = usuarioService;
    }

    /**
     * Executado antes de qualquer handler neste controller.
     * Adiciona "currentPage" = "dashboard" ao model automaticamente.
     * O template da navbar usa este valor para aplicar a classe CSS "active".
     */
    @ModelAttribute("currentPage")
    public String currentPage() {
        return "dashboard";
    }

    /** Adiciona o usuário logado ao model para exibir na navbar */
    @ModelAttribute("usuario")
    public Usuario usuarioLogado() {
        return usuarioService.getUsuarioLogado();
    }

    /**
     * GET / → exibe o dashboard.
     *
     * Comportamento por role:
     * - ADMIN: dashboard global (todas empresas, todos clientes)
     * - GESTOR/USUARIO: dashboard da empresa do usuário
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        Usuario usuario = usuarioService.getUsuarioLogado();

        if (usuario != null) {
            model.addAttribute("role", usuario.getRole().name());

            if (usuario.getRole() == Role.ADMIN) {
                // ADMIN: dashboard global com totais de toda a plataforma
                model.addAttribute("dashboard", empresaService.buscarDashboard());
            } else if (usuario.getEmpresa() != null) {
                // GESTOR/USUARIO: dados da sua empresa
                model.addAttribute("empresaNome", usuario.getEmpresa().getNome());
                model.addAttribute("empresaId", usuario.getEmpresa().getId());
                model.addAttribute("dashboard", empresaService.buscarDashboardEmpresa(usuario.getEmpresa().getId()));
            }
        } else {
            // Fallback sem autenticação (não deveria acontecer, mas seguro)
            model.addAttribute("dashboard", empresaService.buscarDashboard());
            model.addAttribute("role", "ADMIN");
        }

        return "dashboard"; // → src/main/resources/templates/dashboard.html
    }
}
