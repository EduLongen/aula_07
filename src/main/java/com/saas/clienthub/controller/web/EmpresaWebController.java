package com.saas.clienthub.controller.web;

import com.saas.clienthub.model.dto.EmpresaRequestDTO;
import com.saas.clienthub.model.entity.Empresa;
import com.saas.clienthub.model.entity.Plano;
import com.saas.clienthub.service.ClienteService;
import com.saas.clienthub.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller Web para as páginas HTML de Empresa (renderizadas pelo Thymeleaf).
 *
 * =====================================================================
 * CONCEITO: Diferença entre Controller Web e REST Controller
 * =====================================================================
 * Este controller (@Controller) serve páginas HTML para o BROWSER.
 * O EmpresaRestController (@RestController) serve JSON para clientes de API.
 *
 * Ambos usam os MESMOS services — a lógica de negócio não se repete.
 * A diferença está apenas no formato de saída: HTML vs JSON.
 *
 * =====================================================================
 * CONCEITO: PRG Pattern (Post-Redirect-Get)
 * =====================================================================
 * Após um POST (salvar/desativar), fazemos REDIRECT em vez de retornar
 * diretamente um template. Isso evita o problema de "reenvio de formulário"
 * quando o usuário atualiza a página (F5) após um POST.
 *
 * Fluxo PRG:
 *   1. Browser envia POST /empresas/salvar
 *   2. Server processa e responde com 302 Redirect → /empresas
 *   3. Browser faz GET /empresas automaticamente
 *   4. Se o usuário pressionar F5, repete apenas o GET (inofensivo)
 *
 * =====================================================================
 * CONCEITO: Flash Attributes (RedirectAttributes)
 * =====================================================================
 * Mensagens de sucesso/erro precisam sobreviver ao redirect.
 * RedirectAttributes.addFlashAttribute() salva o atributo na sessão
 * temporariamente. Ele é consumido na próxima requisição e removido.
 * O template acessa via ${mensagemSucesso} e ${mensagemErro}.
 *
 * =====================================================================
 * CONCEITO: BindingResult
 * =====================================================================
 * Quando @Valid falha em um @ModelAttribute, o Spring preenche o
 * BindingResult com os erros. Diferente do @RequestBody (REST),
 * aqui precisamos tratar o BindingResult MANUALMENTE — se tiver erros,
 * retornamos o formulário com as mensagens de validação visíveis.
 */
@Controller
@RequestMapping("/empresas")
public class EmpresaWebController {

    private final EmpresaService empresaService;
    private final ClienteService clienteService;

    public EmpresaWebController(EmpresaService empresaService, ClienteService clienteService) {
        this.empresaService = empresaService;
        this.clienteService = clienteService;
    }

    /** Adiciona "currentPage" = "empresas" ao model de todos os handlers desta classe */
    @ModelAttribute("currentPage")
    public String currentPage() {
        return "empresas";
    }

    /**
     * GET /empresas → lista todas as empresas.
     * return "empresa/lista" → renderiza /templates/empresa/lista.html
     */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empresas", empresaService.listarTodas());
        return "empresa/lista";
    }

    /**
     * GET /empresas/novo → exibe o formulário de cadastro.
     * Adiciona um DTO vazio ao model para o Thymeleaf fazer o binding com th:object.
     * Adiciona os valores do enum Plano para popular o <select>.
     */
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("empresa", new EmpresaRequestDTO()); // DTO vazio para o form
        model.addAttribute("planos", Plano.values());           // BASICO, PROFISSIONAL, ENTERPRISE
        return "empresa/formulario";
    }

    /**
     * POST /empresas/salvar → processa o formulário de cadastro/edição.
     *
     * @Valid                     → valida o DTO com as anotações Bean Validation
     * @ModelAttribute("empresa") → popula o DTO com os campos do formulário HTML
     * BindingResult result       → contém erros de validação (DEVE vir logo após o DTO)
     */
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("empresa") EmpresaRequestDTO dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        // Se houver erros de validação, volta para o formulário com as mensagens visíveis
        if (result.hasErrors()) {
            model.addAttribute("planos", Plano.values()); // necessário repassar para o select
            return "empresa/formulario";
        }

        try {
            // Se o hidden field "id" foi enviado → atualização. Caso contrário → criação.
            if (dto.getId() != null) {
                empresaService.atualizar(dto.getId(), dto);
            } else {
                empresaService.criar(dto);
            }
            // Flash attribute: mensagem visível na próxima requisição (após redirect)
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Empresa salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        // PRG Pattern: redireciona para a lista após salvar
        return "redirect:/empresas";
    }

    /**
     * GET /empresas/{id} → exibe a página de detalhes da empresa com seus clientes.
     * Carrega tanto os dados da empresa quanto a lista de clientes do tenant.
     */
    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("empresa", empresaService.buscarPorId(id));
        model.addAttribute("clientes", clienteService.listarPorEmpresa(id));
        return "empresa/detalhes";
    }

    /**
     * GET /empresas/{id}/editar → exibe o formulário preenchido com os dados atuais.
     * Converte a entidade Empresa → EmpresaRequestDTO para popular o formulário.
     * Inclui o ID como campo hidden para que o /salvar saiba que é uma edição.
     */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Empresa empresa = empresaService.buscarEntidadePorId(id);
        // Converte entidade → DTO de request para usar no formulário
        EmpresaRequestDTO dto = EmpresaRequestDTO.builder()
                .id(empresa.getId())
                .nome(empresa.getNome())
                .cnpj(empresa.getCnpj())
                .email(empresa.getEmail())
                .plano(empresa.getPlano())
                .build();
        model.addAttribute("empresa", dto);
        model.addAttribute("planos", Plano.values());
        return "empresa/formulario"; // reutiliza o mesmo template de criação
    }

    /** POST /empresas/{id}/desativar → soft delete via formulário HTML */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        empresaService.desativar(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Empresa desativada com sucesso!");
        return "redirect:/empresas"; // PRG
    }

    /** POST /empresas/{id}/ativar → reativa uma empresa desativada */
    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        empresaService.ativar(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Empresa ativada com sucesso!");
        return "redirect:/empresas"; // PRG
    }
}
