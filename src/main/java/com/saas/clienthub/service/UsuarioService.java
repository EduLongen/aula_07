package com.saas.clienthub.service;

import com.saas.clienthub.exception.BusinessException;
import com.saas.clienthub.exception.ResourceNotFoundException;
import com.saas.clienthub.model.dto.UsuarioRequestDTO;
import com.saas.clienthub.model.dto.UsuarioResponseDTO;
import com.saas.clienthub.model.entity.Empresa;
import com.saas.clienthub.model.entity.Role;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.EmpresaRepository;
import com.saas.clienthub.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service com a lógica de negócio relacionada aos Usuários.
 *
 * =====================================================================
 * CONCEITO: SecurityContextHolder
 * =====================================================================
 * O Spring Security armazena as informações do usuário autenticado
 * no SecurityContextHolder — um ThreadLocal que persiste durante toda
 * a requisição HTTP. Podemos acessar em qualquer ponto do código:
 *
 *   SecurityContextHolder.getContext().getAuthentication()
 *
 * Isso retorna o Authentication, que contém:
 *   - getName() → o username (no nosso caso, o email)
 *   - getAuthorities() → as roles (ROLE_ADMIN, ROLE_GESTOR, etc.)
 *
 * =====================================================================
 * CONCEITO: PasswordEncoder (BCrypt)
 * =====================================================================
 * O PasswordEncoder é injetado como dependência (definido como @Bean
 * no SecurityConfig). Usamos encode() para criar o hash ao salvar
 * e matches() para verificar no login (feito pelo Spring Security).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    /** Injeção via construtor — Spring injeta as dependências automaticamente */
    public UsuarioService(UsuarioRepository usuarioRepository,
                          EmpresaRepository empresaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lista todos os usuários do sistema — usado apenas pelo ADMIN */
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /** Busca um usuário pelo ID e retorna como DTO */
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
        return toResponseDTO(usuario);
    }

    /**
     * Busca a entidade Usuario pelo email — uso interno (autenticação).
     * Retorna a entidade diretamente, não o DTO.
     */
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
    }

    /**
     * Busca a entidade Usuario pelo ID — uso interno.
     * Retorna a entidade diretamente para modificações no Service.
     */
    public Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
    }

    /**
     * Cria um novo usuário no sistema.
     *
     * Regras de negócio:
     * 1. Email deve ser único (não pode duplicar)
     * 2. Senha é criptografada com BCrypt antes de salvar
     * 3. Se role = ADMIN → empresa deve ser null
     * 4. Se role = GESTOR ou USUARIO → empresaId é obrigatório
     */
    @Transactional
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        // Valida que a senha foi informada na criação (mínimo 6 caracteres)
        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new BusinessException("Senha é obrigatória para novo usuário");
        }
        if (dto.getSenha().length() < 6) {
            throw new BusinessException("Senha deve ter no mínimo 6 caracteres");
        }

        // Valida email duplicado
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email já cadastrado: " + dto.getEmail());
        }

        // Valida regras de role vs empresa
        validarRoleEmpresa(dto.getRole(), dto.getEmpresaId());

        // Busca a empresa se necessário
        Empresa empresa = null;
        if (dto.getEmpresaId() != null) {
            empresa = empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + dto.getEmpresaId()));
        }

        // Constrói a entidade com senha criptografada
        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha())) // BCrypt hash
                .role(dto.getRole())
                .empresa(empresa)
                .build();

        usuario = usuarioRepository.save(usuario);
        return toResponseDTO(usuario);
    }

    /**
     * Atualiza um usuário existente.
     *
     * Regra especial para senha:
     * - Se a senha vier preenchida → recriptografar com BCrypt
     * - Se a senha vier vazia/null → manter a senha atual do banco
     *
     * Isso permite editar nome/email/role sem precisar digitar a senha novamente.
     */
    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));

        // Verifica se o email pertence a OUTRO usuário
        usuarioRepository.findByEmail(dto.getEmail())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new BusinessException("Email já cadastrado: " + dto.getEmail());
                });

        // Valida regras de role vs empresa
        validarRoleEmpresa(dto.getRole(), dto.getEmpresaId());

        // Busca a empresa se necessário
        Empresa empresa = null;
        if (dto.getEmpresaId() != null) {
            empresa = empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + dto.getEmpresaId()));
        }

        // Atualiza os campos
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setRole(dto.getRole());
        usuario.setEmpresa(empresa);

        // Só recriptografa se uma nova senha foi fornecida
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            if (dto.getSenha().length() < 6) {
                throw new BusinessException("Senha deve ter no mínimo 6 caracteres");
            }
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        usuario = usuarioRepository.save(usuario);
        return toResponseDTO(usuario);
    }

    /** Soft Delete: desativa o usuário sem remover do banco */
    @Transactional
    public void desativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    /**
     * Obtém o usuário atualmente logado a partir do SecurityContextHolder.
     *
     * Retorna null se não houver autenticação (ex: requisições à API pública).
     * Isso é importante porque /api/** é permitAll — não exige login.
     */
    public Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Se não há autenticação ou o usuário é anônimo → retorna null
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        // O username no Spring Security é o email do usuário
        String email = auth.getName();
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    /**
     * Valida as regras de negócio entre Role e Empresa:
     * - ADMIN não pode ter empresa (é global)
     * - GESTOR e USUARIO precisam ter empresa
     */
    private void validarRoleEmpresa(Role role, Long empresaId) {
        if (role == Role.ADMIN && empresaId != null) {
            throw new BusinessException("Administradores não devem estar vinculados a uma empresa");
        }
        if (role != Role.ADMIN && empresaId == null) {
            throw new BusinessException("Gestores e Usuários devem estar vinculados a uma empresa");
        }
    }

    /** Converte a entidade Usuario para o DTO de resposta */
    private UsuarioResponseDTO toResponseDTO(Usuario entity) {
        return UsuarioResponseDTO.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .email(entity.getEmail())
                .role(entity.getRole())
                .empresaId(entity.getEmpresa() != null ? entity.getEmpresa().getId() : null)
                .empresaNome(entity.getEmpresa() != null ? entity.getEmpresa().getNome() : null)
                .ativo(entity.getAtivo())
                .dataCadastro(entity.getDataCadastro())
                .build();
    }
}
