package com.saas.clienthub.service;

import com.saas.clienthub.exception.BusinessException;
import com.saas.clienthub.exception.ResourceNotFoundException;
import com.saas.clienthub.model.dto.TagRequestDTO;
import com.saas.clienthub.model.dto.TagResponseDTO;
import com.saas.clienthub.model.entity.Empresa;
import com.saas.clienthub.model.entity.Role;
import com.saas.clienthub.model.entity.Tag;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.EmpresaRepository;
import com.saas.clienthub.repository.TagRepository;
import com.saas.clienthub.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service com a lógica de negócio das Tags.
 *
 * Toda operação é multi-tenant: o empresaId é sempre validado via verificarAcessoEmpresa.
 * Tags são estritamente isoladas por empresa.
 */
@Service
public class TagService {

    /** Cor padrão usada quando o usuário não informa cor ao criar tag */
    private static final String COR_PADRAO = "#007AFF";

    private final TagRepository tagRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public TagService(TagRepository tagRepository,
                      EmpresaRepository empresaRepository,
                      UsuarioRepository usuarioRepository) {
        this.tagRepository = tagRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Lista todas as tags de uma empresa (ativas e inativas) */
    public List<TagResponseDTO> listarPorEmpresa(Long empresaId) {
        verificarAcessoEmpresa(empresaId);
        return tagRepository.findByEmpresaId(empresaId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /** Lista todas as tags de uma empresa — com paginação */
    public Page<TagResponseDTO> listarPorEmpresa(Long empresaId, Pageable pageable) {
        verificarAcessoEmpresa(empresaId);
        return tagRepository.findByEmpresaId(empresaId, pageable)
                .map(this::toResponseDTO);
    }

    /** Lista apenas tags ativas — usado nos checkboxes do formulário de cliente */
    public List<TagResponseDTO> listarAtivasPorEmpresa(Long empresaId) {
        verificarAcessoEmpresa(empresaId);
        return tagRepository.findByEmpresaIdAndAtivoTrue(empresaId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /** Busca uma tag específica validando tenant */
    public TagResponseDTO buscarPorId(Long empresaId, Long tagId) {
        verificarAcessoEmpresa(empresaId);
        Tag tag = tagRepository.findByIdAndEmpresaId(tagId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada com id: " + tagId));
        return toResponseDTO(tag);
    }

    /**
     * Cria uma nova tag na empresa.
     * Valida nome duplicado antes de salvar (a constraint UNIQUE do banco é o backup).
     */
    @Transactional
    public TagResponseDTO criar(Long empresaId, TagRequestDTO dto) {
        verificarAcessoEmpresa(empresaId);
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + empresaId));

        if (tagRepository.existsByNomeAndEmpresaId(dto.getNome(), empresaId)) {
            throw new BusinessException("Já existe uma tag com este nome nesta empresa: " + dto.getNome());
        }

        Tag tag = toEntity(dto, empresa);
        tag = tagRepository.save(tag);
        return toResponseDTO(tag);
    }

    /** Atualiza nome/cor de uma tag existente */
    @Transactional
    public TagResponseDTO atualizar(Long empresaId, Long tagId, TagRequestDTO dto) {
        verificarAcessoEmpresa(empresaId);
        Tag tag = tagRepository.findByIdAndEmpresaId(tagId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada com id: " + tagId));

        // Se mudou o nome, valida duplicidade
        if (!tag.getNome().equals(dto.getNome())
                && tagRepository.existsByNomeAndEmpresaId(dto.getNome(), empresaId)) {
            throw new BusinessException("Já existe uma tag com este nome nesta empresa: " + dto.getNome());
        }

        tag.setNome(dto.getNome());
        tag.setCor(dto.getCor() != null && !dto.getCor().isBlank() ? dto.getCor() : COR_PADRAO);

        tag = tagRepository.save(tag);
        return toResponseDTO(tag);
    }

    /** Soft delete — mantém associações históricas intactas */
    @Transactional
    public void desativar(Long empresaId, Long tagId) {
        verificarAcessoEmpresa(empresaId);
        Tag tag = tagRepository.findByIdAndEmpresaId(tagId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada com id: " + tagId));
        tag.setAtivo(false);
        tagRepository.save(tag);
    }

    /** Reativar uma tag desativada */
    @Transactional
    public void ativar(Long empresaId, Long tagId) {
        verificarAcessoEmpresa(empresaId);
        Tag tag = tagRepository.findByIdAndEmpresaId(tagId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada com id: " + tagId));
        tag.setAtivo(true);
        tagRepository.save(tag);
    }

    /** Converter — entidade → DTO de resposta */
    TagResponseDTO toResponseDTO(Tag tag) {
        return TagResponseDTO.builder()
                .id(tag.getId())
                .nome(tag.getNome())
                .cor(tag.getCor())
                .empresaId(tag.getEmpresa().getId())
                .ativo(tag.getAtivo())
                .build();
    }

    /** Converter — DTO de request → entidade (cor default se não informada) */
    private Tag toEntity(TagRequestDTO dto, Empresa empresa) {
        return Tag.builder()
                .nome(dto.getNome())
                .cor(dto.getCor() != null && !dto.getCor().isBlank() ? dto.getCor() : COR_PADRAO)
                .empresa(empresa)
                .ativo(true)
                .build();
    }

    private Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return usuarioRepository.findByEmail(auth.getName()).orElse(null);
    }

    private void verificarAcessoEmpresa(Long empresaId) {
        Usuario usuarioLogado = getUsuarioLogado();
        if (usuarioLogado == null || usuarioLogado.getRole() == Role.ADMIN) {
            return;
        }
        if (usuarioLogado.getEmpresa() == null || !usuarioLogado.getEmpresa().getId().equals(empresaId)) {
            throw new AccessDeniedException("Acesso negado a esta empresa");
        }
    }
}
