package com.saas.clienthub.controller.web;

import com.saas.clienthub.model.dto.UsuarioRequestDTO;
import com.saas.clienthub.model.dto.UsuarioResponseDTO;
import com.saas.clienthub.model.entity.Role;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.service.EmpresaService;
import com.saas.clienthub.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller Web para gestão de Usuários — acessível apenas por ADMIN.
 *
 * =====================================================================
 * CONCEITO: Segurança em camadas
 * =====================================================================
 * A proteção de /usuarios/** acontece em DUAS camadas:
 * 1. SecurityConfig: .requestMatchers("/usuarios/**").hasRole("ADMIN")
 *    → Spring Security bloqueia a requisição ANTES de chegar no controller
 * 2. Template: sec:authorize="hasRole('ADMIN')" no link da navbar
 *    → O link nem aparece para não-ADMIN (segurança visual)
 *
 * IMPORTANTE: nunca confie apenas na segurança visual (esconder botões).
 * Um usuário técnico pode acessar a URL diretamente — por isso
 * a proteção no SecurityConfig é essencial.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioWebController {

    private final UsuarioService usuarioService;
    private final EmpresaService empresaService;

    public UsuarioWebController(UsuarioService usuarioService, EmpresaService empresaService) {
        this.usuarioService = usuarioService;
        this.empresaService = empresaService;
    }

    /** Marca a seção ativa na navbar como "usuarios" */
    @ModelAttribute("currentPage")
    public String currentPage() {
        return "usuarios";
    }

    /** Adiciona o usuário logado ao model de todos os handlers (para exibir na navbar) */
    @ModelAttribute("usuario")
    public Usuario usuarioLogado() {
        return usuarioService.getUsuarioLogado();
    }

    /**
     * GET /usuarios → lista todos os usuários do sistema.
     * Apenas ADMIN acessa (protegido pelo SecurityConfig).
     */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuario/lista";
    }

    /**
     * GET /usuarios/novo → formulário de criação de usuário.
     * Passa um DTO vazio, as roles disponíveis e as empresas ativas.
     */
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("usuarioForm", new UsuarioRequestDTO());
        model.addAttribute("roles", Role.values());
        model.addAttribute("empresas", empresaService.listarAtivas());
        return "usuario/formulario";
    }

    /**
     * POST /usuarios/salvar → processa criação ou edição de usuário.
     * Diferencia criação de edição pelo campo id do DTO.
     */
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("usuarioForm") UsuarioRequestDTO dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        // Validação especial: senha obrigatória na criação, opcional na edição
        if (dto.getId() == null && (dto.getSenha() == null || dto.getSenha().isBlank())) {
            result.rejectValue("senha", "NotBlank", "Senha é obrigatória para novo usuário");
        }

        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("empresas", empresaService.listarAtivas());
            return "usuario/formulario";
        }

        try {
            if (dto.getId() != null) {
                usuarioService.atualizar(dto.getId(), dto);
            } else {
                usuarioService.criar(dto);
            }
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuário salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        return "redirect:/usuarios";
    }

    /**
     * GET /usuarios/{id}/editar → formulário preenchido para edição.
     * A senha não é preenchida — o campo fica vazio (deixe vazio para manter).
     */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        UsuarioResponseDTO usuario = usuarioService.buscarPorId(id);

        // Converte Response → Request DTO para o formulário
        UsuarioRequestDTO dto = UsuarioRequestDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .empresaId(usuario.getEmpresaId())
                // senha fica vazia — será mantida se não for alterada
                .build();

        model.addAttribute("usuarioForm", dto);
        model.addAttribute("roles", Role.values());
        model.addAttribute("empresas", empresaService.listarAtivas());
        return "usuario/formulario";
    }

    /** POST /usuarios/{id}/desativar → soft delete do usuário */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        usuarioService.desativar(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuário desativado com sucesso!");
        return "redirect:/usuarios";
    }
}
