package com.saas.clienthub.service;

import com.saas.clienthub.exception.BusinessException;
import com.saas.clienthub.exception.ResourceNotFoundException;
import com.saas.clienthub.model.dto.ClienteRequestDTO;
import com.saas.clienthub.model.dto.ClienteResponseDTO;
import com.saas.clienthub.model.dto.EnderecoViaCepDTO;
import com.saas.clienthub.model.dto.TagResponseDTO;
import com.saas.clienthub.model.entity.Cliente;
import com.saas.clienthub.model.entity.Empresa;
import com.saas.clienthub.model.entity.Role;
import com.saas.clienthub.model.entity.Tag;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.ClienteRepository;
import com.saas.clienthub.repository.EmpresaRepository;
import com.saas.clienthub.repository.TagRepository;
import com.saas.clienthub.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service com toda a lógica de negócio relacionada aos Clientes.
 *
 * =====================================================================
 * CONCEITO: Paginação
 * =====================================================================
 * Métodos de listagem recebem Pageable e retornam Page<ClienteResponseDTO>.
 * O Pageable encapsula page, size e sort. Page contém os dados da página
 * + metadados (totalElements, totalPages, etc).
 *
 * =====================================================================
 * CONCEITO: Associação de Tags (ManyToMany)
 * =====================================================================
 * Ao criar/atualizar cliente, recebemos um Set<Long> de tagIds.
 * Buscamos as tags via findByIdInAndEmpresaId — que garante que TODAS
 * pertencem à mesma empresa (anti-IDOR). Se alguma tag não for encontrada,
 * lançamos BusinessException (cliente envia ID inválido ou de outra empresa).
 */
@Service
public class ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TagRepository tagRepository;
    private final ViaCepService viaCepService;

    public ClienteService(ClienteRepository clienteRepository,
                          EmpresaRepository empresaRepository,
                          UsuarioRepository usuarioRepository,
                          TagRepository tagRepository,
                          ViaCepService viaCepService) {
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tagRepository = tagRepository;
        this.viaCepService = viaCepService;
    }

    /**
     * Lista todos os clientes de uma empresa — sem paginação.
     * Usa JOIN FETCH para carregar empresa + tags em uma única query (evita N+1).
     * Mantido para usos internos (ex: detalhes da empresa).
     */
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarPorEmpresa(Long empresaId) {
        verificarAcessoEmpresa(empresaId);
        return clienteRepository.findByEmpresaIdWithTags(empresaId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Listagem paginada — principal método usado nas telas de listagem.
     * O Pageable controla page, size e ordenação (vem do controller via @RequestParam).
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> listarPorEmpresa(Long empresaId, Pageable pageable) {
        verificarAcessoEmpresa(empresaId);
        return clienteRepository.findByEmpresaId(empresaId, pageable)
                .map(this::toResponseDTO);
    }

    /**
     * Busca um cliente garantindo que pertence à empresa.
     * Usa JOIN FETCH para já trazer as tags carregadas — evita LazyInitializationException
     * ao mapear no toResponseDTO fora do contexto transacional.
     */
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long empresaId, Long clienteId) {
        verificarAcessoEmpresa(empresaId);
        Cliente cliente = clienteRepository.findByIdAndEmpresaIdWithTags(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
        return toResponseDTO(cliente);
    }

    /**
     * Cria um novo cliente na empresa, associando as tags informadas.
     */
    @Transactional
    public ClienteResponseDTO criar(Long empresaId, ClienteRequestDTO dto) {
        verificarAcessoEmpresa(empresaId);
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + empresaId));

        clienteRepository.findByEmailAndEmpresaId(dto.getEmail(), empresaId)
                .ifPresent(c -> {
                    throw new BusinessException("Email já cadastrado nesta empresa: " + dto.getEmail());
                });

        Cliente cliente = toEntity(dto, empresa);
        preencherEndereco(cliente, dto.getCep());
        cliente.setTags(resolverTags(dto.getTagIds(), empresaId));
        cliente = clienteRepository.save(cliente);
        return toResponseDTO(cliente);
    }

    /**
     * Atualiza dados do cliente + substitui o conjunto de tags.
     * Se tagIds vier nulo/vazio → o cliente fica sem tags (limpa as existentes).
     */
    @Transactional
    public ClienteResponseDTO atualizar(Long empresaId, Long clienteId, ClienteRequestDTO dto) {
        verificarAcessoEmpresa(empresaId);
        Cliente cliente = clienteRepository.findByIdAndEmpresaIdWithTags(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));

        clienteRepository.findByEmailAndEmpresaId(dto.getEmail(), empresaId)
                .filter(c -> !c.getId().equals(clienteId))
                .ifPresent(c -> {
                    throw new BusinessException("Email já cadastrado nesta empresa: " + dto.getEmail());
                });

        cliente.setNome(dto.getNome());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setCep(dto.getCep());
        preencherEndereco(cliente, dto.getCep());

        // Substitui o conjunto completo de tags — mais simples e seguro que diff manual
        cliente.getTags().clear();
        cliente.getTags().addAll(resolverTags(dto.getTagIds(), empresaId));

        cliente = clienteRepository.save(cliente);
        return toResponseDTO(cliente);
    }

    /** Soft Delete */
    @Transactional
    public void desativar(Long empresaId, Long clienteId) {
        verificarAcessoEmpresa(empresaId);
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    /** Reativa cliente desativado */
    @Transactional
    public void ativar(Long empresaId, Long clienteId) {
        verificarAcessoEmpresa(empresaId);
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
        cliente.setAtivo(true);
        clienteRepository.save(cliente);
    }

    /** Pesquisa paginada por nome dentro de uma empresa */
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> pesquisarPorNome(Long empresaId, String nome, Pageable pageable) {
        verificarAcessoEmpresa(empresaId);
        return clienteRepository.findByEmpresaIdAndNomeContainingIgnoreCase(empresaId, nome, pageable)
                .map(this::toResponseDTO);
    }

    /** Versão sem paginação — mantida para API REST antiga que ainda retorna List */
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> pesquisarPorNome(Long empresaId, String nome) {
        verificarAcessoEmpresa(empresaId);
        return clienteRepository.findByEmpresaIdAndNomeContainingIgnoreCase(empresaId, nome).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Resolve os IDs de tag em entidades, validando que TODAS pertencem à empresa.
     * Se algum ID for inválido ou pertencer a outra empresa, lança BusinessException.
     */
    private Set<Tag> resolverTags(Set<Long> tagIds, Long empresaId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> encontradas = tagRepository.findByIdInAndEmpresaId(tagIds, empresaId);
        if (encontradas.size() != tagIds.size()) {
            throw new BusinessException("Uma ou mais tags não foram encontradas ou não pertencem a esta empresa");
        }
        return new HashSet<>(encontradas);
    }

    private void preencherEndereco(Cliente cliente, String cep) {
        if (cep != null && !cep.isBlank()) {
            try {
                EnderecoViaCepDTO endereco = viaCepService.buscarCep(cep);
                cliente.setLogradouro(endereco.getLogradouro());
                cliente.setBairro(endereco.getBairro());
                cliente.setCidade(endereco.getLocalidade());
                cliente.setUf(endereco.getUf());
            } catch (Exception e) {
                log.warn("Erro ao buscar CEP {}: {}", cep, e.getMessage());
            }
        }
    }

    private ClienteResponseDTO toResponseDTO(Cliente entity) {
        // Mapeia tags para DTOs — se null (cliente novo sem tags), usa lista vazia
        List<TagResponseDTO> tagDtos = entity.getTags() == null
                ? new ArrayList<>()
                : entity.getTags().stream()
                    .map(tag -> TagResponseDTO.builder()
                            .id(tag.getId())
                            .nome(tag.getNome())
                            .cor(tag.getCor())
                            .empresaId(entity.getEmpresa().getId())
                            .ativo(tag.getAtivo())
                            .build())
                    .toList();

        return ClienteResponseDTO.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .email(entity.getEmail())
                .telefone(entity.getTelefone())
                .cep(entity.getCep())
                .logradouro(entity.getLogradouro())
                .bairro(entity.getBairro())
                .cidade(entity.getCidade())
                .uf(entity.getUf())
                .ativo(entity.getAtivo())
                .dataCadastro(entity.getDataCadastro())
                .empresaId(entity.getEmpresa().getId())
                .empresaNome(entity.getEmpresa().getNome())
                .tags(tagDtos)
                .build();
    }

    private Cliente toEntity(ClienteRequestDTO dto, Empresa empresa) {
        return Cliente.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .telefone(dto.getTelefone())
                .cep(dto.getCep())
                .empresa(empresa)
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
