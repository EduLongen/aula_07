package com.saas.clienthub.controller.web;

import com.saas.clienthub.model.dto.ClienteRequestDTO;
import com.saas.clienthub.model.dto.ClienteResponseDTO;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.service.ClienteService;
import com.saas.clienthub.service.EmpresaService;
import com.saas.clienthub.service.TagService;
import com.saas.clienthub.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Controller Web para as páginas HTML de Cliente.
 *
 * =====================================================================
 * CONCEITO: Paginação no formulário web
 * =====================================================================
 * A listagem recebe page/size via @RequestParam e passa o Page para o template.
 * O template usa totalPages e currentPage para renderizar a barra de paginação.
 *
 * =====================================================================
 * CONCEITO: Tags no formulário de cliente
 * =====================================================================
 * Ao abrir o formulário (novo ou editar), carregamos as tags ATIVAS da empresa
 * para renderizar os checkboxes. Na edição, preenchemos dto.tagIds com os IDs
 * das tags que o cliente já tem — o Thymeleaf marca os checkboxes correspondentes.
 */
@Controller
@RequestMapping("/empresas/{empresaId}/clientes")
public class ClienteWebController {

    private final ClienteService clienteService;
    private final EmpresaService empresaService;
    private final TagService tagService;
    private final UsuarioService usuarioService;

    public ClienteWebController(ClienteService clienteService,
                                EmpresaService empresaService,
                                TagService tagService,
                                UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.empresaService = empresaService;
        this.tagService = tagService;
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("currentPage")
    public String currentPage() {
        return "empresas";
    }

    @ModelAttribute("usuario")
    public Usuario usuarioLogado() {
        return usuarioService.getUsuarioLogado();
    }

    /**
     * GET /empresas/{empresaId}/clientes?page=0&size=10
     * Renderiza a lista paginada e passa os metadados de paginação para o template.
     */
    @GetMapping
    public String listar(@PathVariable Long empresaId,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "5") int size,
                         @RequestParam(required = false) String nome,
                         Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome"));

        // Se veio "nome" do campo de pesquisa, usa o método de pesquisa paginada
        Page<ClienteResponseDTO> clientesPage = (nome != null && !nome.isBlank())
                ? clienteService.pesquisarPorNome(empresaId, nome, pageable)
                : clienteService.listarPorEmpresa(empresaId, pageable);

        model.addAttribute("clientesPage", clientesPage);
        model.addAttribute("clientes", clientesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clientesPage.getTotalPages());
        model.addAttribute("totalItems", clientesPage.getTotalElements());
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        model.addAttribute("nomePesquisa", nome);
        return "cliente/lista";
    }

    /** GET /empresas/{empresaId}/clientes/novo — formulário de novo cliente com lista de tags */
    @GetMapping("/novo")
    public String novo(@PathVariable Long empresaId, Model model) {
        model.addAttribute("cliente", new ClienteRequestDTO());
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        model.addAttribute("tagsDisponiveis", tagService.listarAtivasPorEmpresa(empresaId));
        return "cliente/formulario";
    }

    /** POST /empresas/{empresaId}/clientes/salvar — cria ou edita */
    @PostMapping("/salvar")
    public String salvar(@PathVariable Long empresaId,
                         @Valid @ModelAttribute("cliente") ClienteRequestDTO dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
            model.addAttribute("tagsDisponiveis", tagService.listarAtivasPorEmpresa(empresaId));
            return "cliente/formulario";
        }

        try {
            if (dto.getId() != null) {
                clienteService.atualizar(empresaId, dto.getId(), dto);
            } else {
                clienteService.criar(empresaId, dto);
            }
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Cliente salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        return "redirect:/empresas/" + empresaId;
    }

    /** GET /empresas/{empresaId}/clientes/{id}/editar — preenche dto + pré-marca checkboxes */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long empresaId, @PathVariable Long id, Model model) {
        ClienteResponseDTO cliente = clienteService.buscarPorId(empresaId, id);

        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .id(cliente.getId())
                .nome(cliente.getNome())
                .email(cliente.getEmail())
                .telefone(cliente.getTelefone())
                .cep(cliente.getCep())
                // IDs das tags atuais — o Thymeleaf marca os checkboxes correspondentes
                .tagIds(cliente.getTags() == null ? new HashSet<>()
                        : cliente.getTags().stream()
                            .map(t -> t.getId())
                            .collect(Collectors.toSet()))
                .build();

        model.addAttribute("cliente", dto);
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        model.addAttribute("tagsDisponiveis", tagService.listarAtivasPorEmpresa(empresaId));
        return "cliente/formulario";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long empresaId, @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        clienteService.desativar(empresaId, id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Cliente desativado com sucesso!");
        return "redirect:/empresas/" + empresaId;
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long empresaId, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        clienteService.ativar(empresaId, id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Cliente ativado com sucesso!");
        return "redirect:/empresas/" + empresaId;
    }
}
