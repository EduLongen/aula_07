package com.saas.clienthub.controller.web;

import com.saas.clienthub.model.dto.ClienteRequestDTO;
import com.saas.clienthub.model.dto.ClienteResponseDTO;
import com.saas.clienthub.service.ClienteService;
import com.saas.clienthub.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller Web para as páginas HTML de Cliente (renderizadas pelo Thymeleaf).
 *
 * =====================================================================
 * CONCEITO: URLs com Múltiplas Variáveis de Path
 * =====================================================================
 * A URL base é: /empresas/{empresaId}/clientes
 *
 * O empresaId está na URL por dois motivos:
 * 1. Contexto visual: o usuário sabe em qual empresa está navegando
 * 2. Segurança: toda operação de cliente exige o empresaId,
 *    garantindo o isolamento multi-tenant também nas páginas web
 *
 * =====================================================================
 * CONCEITO: Reutilização de Templates
 * =====================================================================
 * O formulário de criação e edição usam o MESMO template (cliente/formulario.html).
 * A distinção entre criar/editar é feita pelo campo hidden "id" no formulário:
 *   - id = null → criação (POST /salvar)
 *   - id = 42   → edição (POST /salvar com id preenchido → service chama atualizar)
 *
 * =====================================================================
 * CONCEITO: Por que o /salvar verifica dto.getId()?
 * =====================================================================
 * Como usamos o mesmo endpoint POST /salvar para criar E editar,
 * o controller decide qual operação executar baseado no ID:
 *   - dto.getId() == null → clienteService.criar(empresaId, dto)
 *   - dto.getId() != null → clienteService.atualizar(empresaId, dto.getId(), dto)
 */
@Controller
@RequestMapping("/empresas/{empresaId}/clientes")
public class ClienteWebController {

    private final ClienteService clienteService;
    private final EmpresaService empresaService;

    public ClienteWebController(ClienteService clienteService, EmpresaService empresaService) {
        this.clienteService = clienteService;
        this.empresaService = empresaService;
    }

    /** Garante que todos os templates desta classe saibam que estamos na seção "empresas" */
    @ModelAttribute("currentPage")
    public String currentPage() {
        return "empresas";
    }

    /**
     * GET /empresas/{empresaId}/clientes → lista clientes da empresa.
     * Passa tanto a lista de clientes quanto os dados da empresa para o template.
     */
    @GetMapping
    public String listar(@PathVariable Long empresaId, Model model) {
        model.addAttribute("clientes", clienteService.listarPorEmpresa(empresaId));
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId)); // para exibir nome da empresa
        return "cliente/lista";
    }

    /** GET /empresas/{empresaId}/clientes/novo → exibe formulário de novo cliente */
    @GetMapping("/novo")
    public String novo(@PathVariable Long empresaId, Model model) {
        model.addAttribute("cliente", new ClienteRequestDTO()); // DTO vazio para o form
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        return "cliente/formulario";
    }

    /**
     * POST /empresas/{empresaId}/clientes/salvar → processa criação ou edição.
     *
     * Nota: empresaId vem do @PathVariable da URL, não do formulário.
     * Isso evita que um usuário malicioso altere o empresaId no HTML
     * e consiga salvar um cliente em outra empresa.
     */
    @PostMapping("/salvar")
    public String salvar(@PathVariable Long empresaId,
                         @Valid @ModelAttribute("cliente") ClienteRequestDTO dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        // Erros de validação → volta para o formulário com mensagens de erro
        if (result.hasErrors()) {
            model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
            return "cliente/formulario";
        }

        try {
            if (dto.getId() != null) {
                clienteService.atualizar(empresaId, dto.getId(), dto); // edição
            } else {
                clienteService.criar(empresaId, dto); // criação
            }
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Cliente salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        // Redireciona para a página de detalhes da empresa (PRG Pattern)
        return "redirect:/empresas/" + empresaId;
    }

    /**
     * GET /empresas/{empresaId}/clientes/{id}/editar → formulário preenchido para edição.
     * Converte o ResponseDTO para um RequestDTO (que tem o campo id para indicar edição).
     */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long empresaId, @PathVariable Long id, Model model) {
        ClienteResponseDTO cliente = clienteService.buscarPorId(empresaId, id);

        // Converte Response → Request DTO para usar no formulário de edição
        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .id(cliente.getId())         // ID incluído → indica edição no /salvar
                .nome(cliente.getNome())
                .email(cliente.getEmail())
                .telefone(cliente.getTelefone())
                .cep(cliente.getCep())
                .build();

        model.addAttribute("cliente", dto);
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        return "cliente/formulario"; // reutiliza o mesmo template
    }

    /** POST /empresas/{empresaId}/clientes/{id}/desativar → soft delete do cliente */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long empresaId, @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        clienteService.desativar(empresaId, id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Cliente desativado com sucesso!");
        return "redirect:/empresas/" + empresaId; // PRG
    }

    /** POST /empresas/{empresaId}/clientes/{id}/ativar → reativa cliente desativado */
    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long empresaId, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        clienteService.ativar(empresaId, id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Cliente ativado com sucesso!");
        return "redirect:/empresas/" + empresaId; // PRG
    }
}
