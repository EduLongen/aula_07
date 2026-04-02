package com.saas.clienthub.controller.web;

import com.saas.clienthub.service.EmpresaService;
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

    public DashboardController(EmpresaService empresaService) {
        this.empresaService = empresaService;
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

    /**
     * GET / → exibe o dashboard com os totais da plataforma.
     *
     * model.addAttribute("dashboard", ...) → o template acessa com ${dashboard.totalEmpresas}, etc.
     * return "dashboard" → o Spring renderiza /templates/dashboard.html
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", empresaService.buscarDashboard());
        return "dashboard"; // → src/main/resources/templates/dashboard.html
    }
}
