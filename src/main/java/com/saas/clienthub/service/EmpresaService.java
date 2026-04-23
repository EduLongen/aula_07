package com.saas.clienthub.service;

import com.saas.clienthub.exception.BusinessException;
import com.saas.clienthub.exception.ResourceNotFoundException;
import com.saas.clienthub.model.dto.DashboardDTO;
import com.saas.clienthub.model.dto.EmpresaRequestDTO;
import com.saas.clienthub.model.dto.EmpresaResponseDTO;
import com.saas.clienthub.model.entity.Empresa;
import com.saas.clienthub.model.entity.Plano;
import com.saas.clienthub.model.entity.Role;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.ClienteRepository;
import com.saas.clienthub.repository.EmpresaRepository;
import com.saas.clienthub.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service com toda a lógica de negócio relacionada às Empresas.
 *
 * =====================================================================
 * CONCEITO: Camada de Service
 * =====================================================================
 * O Service é a camada responsável pelas regras de negócio da aplicação.
 * Ele fica entre o Controller (que recebe as requisições) e o Repository
 * (que acessa o banco). Responsabilidades do Service:
 *   - Validar regras de negócio (ex: CNPJ duplicado)
 *   - Orquestrar múltiplas operações de repositório
 *   - Converter entre entidades JPA e DTOs
 *   - Gerenciar transações com @Transactional
 *
 * =====================================================================
 * CONCEITO: @Transactional
 * =====================================================================
 * Garante que todas as operações de banco dentro do método sejam
 * executadas em uma única transação ACID:
 *   - Se tudo der certo → COMMIT (salva as mudanças)
 *   - Se ocorrer uma exceção → ROLLBACK (desfaz tudo)
 *
 * Apenas métodos de ESCRITA precisam de @Transactional.
 * Métodos de leitura (GET) não precisam — são operações de consulta apenas.
 *
 * =====================================================================
 * CONCEITO: DTO Pattern (Data Transfer Object)
 * =====================================================================
 * Nunca expor a entidade JPA diretamente na API. Usamos DTOs para:
 *   - Controlar exatamente quais campos são expostos ao cliente
 *   - Ter validações específicas para entrada (Request) e saída (Response)
 *   - Desacoplar o modelo de banco do contrato da API
 *
 * RequestDTO → dados que chegam do cliente
 * ResponseDTO → dados que retornamos ao cliente
 */
@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Injeção via construtor.
     * O ClienteRepository é necessário para contar clientes no método toResponseDTO.
     * O UsuarioRepository é necessário para verificar o tenant do usuário logado.
     */
    public EmpresaService(EmpresaRepository empresaRepository,
                          ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Retorna empresas convertidas para DTO.
     *
     * Isolamento multi-tenant:
     * - ADMIN: vê todas as empresas
     * - GESTOR/USUARIO: vê apenas a própria empresa
     * - Sem autenticação (API pública): vê todas (JWT futuro restringirá)
     */
    public List<EmpresaResponseDTO> listarTodas() {
        Usuario usuarioLogado = getUsuarioLogado();

        // Se não há usuário logado (API pública) ou é ADMIN → retorna todas
        if (usuarioLogado == null || usuarioLogado.getRole() == Role.ADMIN) {
            return empresaRepository.findAll().stream()
                    .map(this::toResponseDTO)
                    .toList();
        }

        // GESTOR/USUARIO → retorna apenas a empresa do usuário
        if (usuarioLogado.getEmpresa() != null) {
            return List.of(toResponseDTO(usuarioLogado.getEmpresa()));
        }

        return List.of();
    }

    /**
     * Listagem paginada de empresas (respeitando multi-tenancy).
     * ADMIN → todas empresas paginadas.
     * GESTOR/USUARIO → apenas a sua empresa (como única página com 1 item).
     */
    public Page<EmpresaResponseDTO> listarTodas(Pageable pageable) {
        Usuario usuarioLogado = getUsuarioLogado();

        if (usuarioLogado == null || usuarioLogado.getRole() == Role.ADMIN) {
            return empresaRepository.findAll(pageable).map(this::toResponseDTO);
        }

        // Não-ADMIN → retorna apenas a própria empresa como Page simples
        if (usuarioLogado.getEmpresa() != null) {
            List<EmpresaResponseDTO> lista = List.of(toResponseDTO(usuarioLogado.getEmpresa()));
            return new PageImpl<>(lista, pageable, lista.size());
        }

        return new PageImpl<>(List.of(), pageable, 0);
    }

    /** Retorna apenas empresas com ativa = true */
    public List<EmpresaResponseDTO> listarAtivas() {
        return empresaRepository.findByAtivaTrue().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Busca uma empresa pelo ID e retorna como DTO.
     *
     * Isolamento multi-tenant:
     * - ADMIN ou sem autenticação: pode ver qualquer empresa
     * - GESTOR/USUARIO: só pode ver a própria empresa
     */
    public EmpresaResponseDTO buscarPorId(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + id));

        // Verifica se o usuário logado tem permissão para ver esta empresa
        verificarAcessoEmpresa(id);

        return toResponseDTO(empresa);
    }

    /**
     * Retorna a entidade JPA diretamente (sem converter para DTO).
     * Usado apenas pelos Web Controllers para preencher formulários de edição.
     * Controllers REST NUNCA devem chamar este método — só usam DTOs.
     */
    public Empresa buscarEntidadePorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + id));
    }

    /**
     * Cria uma nova empresa no banco.
     *
     * @Transactional garante que se qualquer operação falhar,
     * o banco volta ao estado anterior (rollback).
     */
    @Transactional
    public EmpresaResponseDTO criar(EmpresaRequestDTO dto) {
        // Regra de negócio: CNPJ deve ser único no sistema
        if (empresaRepository.existsByCnpj(dto.getCnpj())) {
            // BusinessException → HTTP 422 (Unprocessable Entity)
            // Significa que os dados são válidos mas a regra de negócio não permite
            throw new BusinessException("CNPJ já cadastrado: " + dto.getCnpj());
        }

        Empresa empresa = toEntity(dto);            // DTO → Entidade
        empresa = empresaRepository.save(empresa);  // INSERT no banco
        return toResponseDTO(empresa);              // Entidade → DTO de resposta
    }

    /**
     * Atualiza os dados de uma empresa existente.
     * Valida CNPJ duplicado, excluindo a própria empresa da verificação
     * (afinal, ela pode manter o mesmo CNPJ).
     */
    @Transactional
    public EmpresaResponseDTO atualizar(Long id, EmpresaRequestDTO dto) {
        // Busca a empresa — lança 404 se não existir
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + id));

        // Verifica se o CNPJ já pertence a OUTRA empresa
        // filter(e -> !e.getId().equals(id)) → ignora a própria empresa
        // ifPresent → executa o bloco se encontrar outra empresa com o CNPJ
        empresaRepository.findByCnpj(dto.getCnpj())
                .filter(e -> !e.getId().equals(id))
                .ifPresent(e -> {
                    throw new BusinessException("CNPJ já cadastrado: " + dto.getCnpj());
                });

        // Atualiza os campos da entidade já gerenciada pelo JPA
        empresa.setNome(dto.getNome());
        empresa.setCnpj(dto.getCnpj());
        empresa.setEmail(dto.getEmail());
        empresa.setPlano(dto.getPlano() != null ? dto.getPlano() : Plano.BASICO);

        // save() com entidade existente → UPDATE no banco (detecta pelo ID)
        empresa = empresaRepository.save(empresa);
        return toResponseDTO(empresa);
    }

    /**
     * Soft Delete: não remove do banco, apenas marca como inativa.
     * Isso preserva o histórico e permite reativar futuramente.
     */
    @Transactional
    public void desativar(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + id));
        empresa.setAtiva(false);  // UPDATE empresas SET ativa = false WHERE id = ?
        empresaRepository.save(empresa);
    }

    /** Reativa uma empresa que estava desativada */
    @Transactional
    public void ativar(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + id));
        empresa.setAtiva(true);   // UPDATE empresas SET ativa = true WHERE id = ?
        empresaRepository.save(empresa);
    }

    /**
     * Monta os dados para o Dashboard da aplicação.
     * Usa queries de COUNT direto no banco — muito mais eficiente
     * do que trazer todos os registros e contar em memória.
     */
    public DashboardDTO buscarDashboard() {
        return DashboardDTO.builder()
                .totalEmpresas(empresaRepository.count())            // COUNT(*) FROM empresas
                .empresasAtivas(empresaRepository.countByAtivaTrue()) // COUNT(*) WHERE ativa = true
                .totalClientes(clienteRepository.count())            // COUNT(*) FROM clientes
                .clientesAtivos(clienteRepository.countByAtivoTrue()) // COUNT(*) WHERE ativo = true
                .build();
    }

    /**
     * Monta os dados do Dashboard para uma empresa específica (GESTOR/USUARIO).
     * Conta apenas os clientes da empresa do usuário logado.
     */
    public DashboardDTO buscarDashboardEmpresa(Long empresaId) {
        return DashboardDTO.builder()
                .totalEmpresas(1)  // o usuário vê apenas a sua empresa
                .empresasAtivas(1)
                .totalClientes(clienteRepository.countByEmpresaId(empresaId))
                .clientesAtivos(clienteRepository.countByEmpresaIdAndAtivoTrue(empresaId))
                .build();
    }

    /**
     * Converte uma entidade Empresa para o DTO de resposta.
     * Método privado — só usado internamente neste Service.
     *
     * Nota: totalClientes faz uma query COUNT no banco para cada empresa.
     * Em listas grandes isso pode ser lento (N+1 queries). Para escalar,
     * usaríamos uma query JOIN com GROUP BY.
     */
    private EmpresaResponseDTO toResponseDTO(Empresa entity) {
        return EmpresaResponseDTO.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .cnpj(entity.getCnpj())
                .email(entity.getEmail())
                .plano(entity.getPlano())
                .ativa(entity.getAtiva())
                .dataCadastro(entity.getDataCadastro())
                // Conta os clientes desta empresa com uma query separada
                .totalClientes(clienteRepository.countByEmpresaId(entity.getId()))
                .build();
    }

    /**
     * Converte o DTO de requisição em uma entidade JPA.
     * ID não é copiado pois é gerado pelo banco.
     * dataCadastro não é copiado pois é preenchido pelo @PrePersist.
     */
    private Empresa toEntity(EmpresaRequestDTO dto) {
        return Empresa.builder()
                .nome(dto.getNome())
                .cnpj(dto.getCnpj())
                .email(dto.getEmail())
                .plano(dto.getPlano() != null ? dto.getPlano() : Plano.BASICO)
                .build();
    }

    /**
     * Obtém o usuário logado a partir do SecurityContextHolder.
     * Retorna null se não houver autenticação (ex: API pública sem login).
     */
    private Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return usuarioRepository.findByEmail(auth.getName()).orElse(null);
    }

    /**
     * Verifica se o usuário logado tem permissão para acessar a empresa informada.
     * ADMIN e requisições sem autenticação (API) podem acessar qualquer empresa.
     * GESTOR/USUARIO só podem acessar a própria empresa.
     */
    private void verificarAcessoEmpresa(Long empresaId) {
        Usuario usuarioLogado = getUsuarioLogado();
        // Sem login (API pública) ou ADMIN → acesso total
        if (usuarioLogado == null || usuarioLogado.getRole() == Role.ADMIN) {
            return;
        }
        // GESTOR/USUARIO → verifica se é a empresa do usuário
        if (usuarioLogado.getEmpresa() == null || !usuarioLogado.getEmpresa().getId().equals(empresaId)) {
            throw new AccessDeniedException("Acesso negado a esta empresa");
        }
    }
}
