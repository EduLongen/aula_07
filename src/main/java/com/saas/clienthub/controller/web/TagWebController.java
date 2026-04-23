package com.saas.clienthub.controller.web;

import com.saas.clienthub.model.dto.TagRequestDTO;
import com.saas.clienthub.model.dto.TagResponseDTO;
import com.saas.clienthub.model.entity.Usuario;
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

/**
 * Controller Web para as páginas HTML de Tag (Thymeleaf).
 * URL base: /empresas/{empresaId}/tags — aninhado por empresa para deixar claro o escopo.
 */
@Controller
@RequestMapping("/empresas/{empresaId}/tags")
public class TagWebController {

    private final TagService tagService;
    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;

    public TagWebController(TagService tagService,
                            EmpresaService empresaService,
                            UsuarioService usuarioService) {
        this.tagService = tagService;
        this.empresaService = empresaService;
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

    /** GET /empresas/{empresaId}/tags → lista paginada de tags da empresa */
    @GetMapping
    public String listar(@PathVariable Long empresaId,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "5") int size,
                         Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome"));
        Page<TagResponseDTO> tagsPage = tagService.listarPorEmpresa(empresaId, pageable);

        model.addAttribute("tagsPage", tagsPage);
        model.addAttribute("tags", tagsPage.getContent());
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        return "tag/lista";
    }

    /** GET /empresas/{empresaId}/tags/novo → formulário nova tag */
    @GetMapping("/novo")
    public String novo(@PathVariable Long empresaId, Model model) {
        TagRequestDTO dto = new TagRequestDTO();
        dto.setCor("#007AFF"); // sugestão padrão para o color picker
        model.addAttribute("tag", dto);
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        return "tag/formulario";
    }

    /** POST /empresas/{empresaId}/tags/salvar → cria ou atualiza */
    @PostMapping("/salvar")
    public String salvar(@PathVariable Long empresaId,
                         @Valid @ModelAttribute("tag") TagRequestDTO dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
            return "tag/formulario";
        }

        try {
            if (dto.getId() != null) {
                tagService.atualizar(empresaId, dto.getId(), dto);
            } else {
                tagService.criar(empresaId, dto);
            }
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Tag salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        return "redirect:/empresas/" + empresaId + "/tags";
    }

    /** GET /empresas/{empresaId}/tags/{id}/editar → formulário preenchido */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long empresaId, @PathVariable Long id, Model model) {
        TagResponseDTO tag = tagService.buscarPorId(empresaId, id);
        TagRequestDTO dto = TagRequestDTO.builder()
                .id(tag.getId())
                .nome(tag.getNome())
                .cor(tag.getCor())
                .build();
        model.addAttribute("tag", dto);
        model.addAttribute("empresa", empresaService.buscarPorId(empresaId));
        return "tag/formulario";
    }

    /** POST /empresas/{empresaId}/tags/{id}/desativar */
    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long empresaId, @PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        tagService.desativar(empresaId, id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Tag desativada com sucesso!");
        return "redirect:/empresas/" + empresaId + "/tags";
    }

    /** POST /empresas/{empresaId}/tags/{id}/ativar */
    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long empresaId, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        tagService.ativar(empresaId, id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Tag ativada com sucesso!");
        return "redirect:/empresas/" + empresaId + "/tags";
    }
}
