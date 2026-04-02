package com.saas.clienthub.service;

import com.saas.clienthub.exception.BusinessException;
import com.saas.clienthub.exception.ResourceNotFoundException;
import com.saas.clienthub.model.dto.ClienteRequestDTO;
import com.saas.clienthub.model.dto.ClienteResponseDTO;
import com.saas.clienthub.model.dto.EnderecoViaCepDTO;
import com.saas.clienthub.model.entity.Cliente;
import com.saas.clienthub.model.entity.Empresa;
import com.saas.clienthub.repository.ClienteRepository;
import com.saas.clienthub.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service com toda a lógica de negócio relacionada aos Clientes.
 *
 * =====================================================================
 * CONCEITO: Logger / SLF4J
 * =====================================================================
 * SLF4J (Simple Logging Facade for Java) é uma abstração de logging.
 * Usamos Logger para registrar mensagens de diagnóstico na aplicação:
 *
 * log.info()  → informação importante (ex: "Dados carregados: X registros")
 * log.warn()  → algo inesperado mas não crítico (ex: "CEP não encontrado")
 * log.error() → erro que impactou o usuário
 * log.debug() → detalhes para desenvolvimento (não aparecem em produção)
 *
 * O Logback (implementação padrão do Spring Boot) escreve os logs no console
 * e pode ser configurado para salvar em arquivo, enviar para serviço externo, etc.
 */
@Service
public class ClienteService {

    // Cria um logger específico para esta classe — aparece nos logs como "[ClienteService]"
    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final ViaCepService viaCepService;  // Dependência para buscar endereço pelo CEP

    /** Injeção via construtor — Spring injeta todas as dependências automaticamente */
    public ClienteService(ClienteRepository clienteRepository,
                          EmpresaRepository empresaRepository,
                          ViaCepService viaCepService) {
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.viaCepService = viaCepService;
    }

    /**
     * Lista todos os clientes de uma empresa específica.
     * Sempre filtra por empresaId — nunca retorna clientes de outras empresas.
     */
    public List<ClienteResponseDTO> listarPorEmpresa(Long empresaId) {
        return clienteRepository.findByEmpresaId(empresaId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Busca um cliente garantindo que pertence à empresa informada.
     * findByIdAndEmpresaId → WHERE id = ? AND empresa_id = ?
     * Se o cliente existir mas for de outra empresa → retorna vazio → HTTP 404
     */
    public ClienteResponseDTO buscarPorId(Long empresaId, Long clienteId) {
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
        return toResponseDTO(cliente);
    }

    /**
     * Cria um novo cliente vinculado a uma empresa.
     *
     * Fluxo:
     * 1. Valida que a empresa existe
     * 2. Valida que o email não está duplicado nesta empresa
     * 3. Converte DTO → Entidade
     * 4. Tenta preencher endereço via CEP (sem falhar se der erro)
     * 5. Salva no banco
     * 6. Retorna DTO de resposta
     */
    @Transactional
    public ClienteResponseDTO criar(Long empresaId, ClienteRequestDTO dto) {
        // Garante que a empresa existe antes de criar o cliente
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com id: " + empresaId));

        // Regra de negócio: email único POR EMPRESA (multi-tenant)
        // O mesmo email pode existir em empresas diferentes
        clienteRepository.findByEmailAndEmpresaId(dto.getEmail(), empresaId)
                .ifPresent(c -> {
                    throw new BusinessException("Email já cadastrado nesta empresa: " + dto.getEmail());
                });

        Cliente cliente = toEntity(dto, empresa);
        preencherEndereco(cliente, dto.getCep()); // Busca endereço pelo CEP se informado
        cliente = clienteRepository.save(cliente);
        return toResponseDTO(cliente);
    }

    /**
     * Atualiza os dados de um cliente existente.
     * Valida email duplicado excluindo o próprio cliente da verificação.
     */
    @Transactional
    public ClienteResponseDTO atualizar(Long empresaId, Long clienteId, ClienteRequestDTO dto) {
        // Busca o cliente garantindo que pertence à empresa — proteção multi-tenant
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));

        // Verifica se o email pertence a OUTRO cliente desta empresa
        clienteRepository.findByEmailAndEmpresaId(dto.getEmail(), empresaId)
                .filter(c -> !c.getId().equals(clienteId)) // ignora o próprio cliente
                .ifPresent(c -> {
                    throw new BusinessException("Email já cadastrado nesta empresa: " + dto.getEmail());
                });

        // Atualiza os campos
        cliente.setNome(dto.getNome());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setCep(dto.getCep());
        preencherEndereco(cliente, dto.getCep()); // Atualiza o endereço se o CEP mudou

        cliente = clienteRepository.save(cliente);
        return toResponseDTO(cliente);
    }

    /** Soft Delete: marca o cliente como inativo sem removê-lo do banco */
    @Transactional
    public void desativar(Long empresaId, Long clienteId) {
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    /** Reativa um cliente previamente desativado */
    @Transactional
    public void ativar(Long empresaId, Long clienteId) {
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
        cliente.setAtivo(true);
        clienteRepository.save(cliente);
    }

    /** Pesquisa clientes por nome dentro de uma empresa (busca parcial, sem case) */
    public List<ClienteResponseDTO> pesquisarPorNome(Long empresaId, String nome) {
        return clienteRepository.findByEmpresaIdAndNomeContainingIgnoreCase(empresaId, nome).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Tenta preencher os dados de endereço do cliente consultando a API ViaCEP.
     *
     * Decisão de design importante: usamos try-catch aqui para NÃO
     * deixar que uma falha na API externa (ViaCEP) impeça o cadastro do cliente.
     * Se o CEP não for encontrado, o cliente é salvo sem endereço — isso é aceitável.
     * O erro é apenas logado como WARNING (não como ERROR, pois não é crítico).
     */
    private void preencherEndereco(Cliente cliente, String cep) {
        if (cep != null && !cep.isBlank()) {
            try {
                EnderecoViaCepDTO endereco = viaCepService.buscarCep(cep);
                // Preenche os campos de endereço com os dados retornados pela API
                cliente.setLogradouro(endereco.getLogradouro());
                cliente.setBairro(endereco.getBairro());
                cliente.setCidade(endereco.getLocalidade()); // ViaCEP usa "localidade" para cidade
                cliente.setUf(endereco.getUf());
            } catch (Exception e) {
                // Falha na API ViaCEP não interrompe o cadastro — apenas loga o aviso
                log.warn("Erro ao buscar CEP {}: {}", cep, e.getMessage());
            }
        }
    }

    /**
     * Converte a entidade Cliente para o DTO de resposta.
     * Inclui dados da empresa (id e nome) sem expor a entidade Empresa completa.
     *
     * Nota: empresa.getId() e empresa.getNome() funcionam aqui porque
     * a entidade Cliente foi carregada com a Empresa em contexto transacional.
     */
    private ClienteResponseDTO toResponseDTO(Cliente entity) {
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
                .empresaId(entity.getEmpresa().getId())    // apenas o ID da empresa
                .empresaNome(entity.getEmpresa().getNome()) // apenas o nome da empresa
                .build();
    }

    /**
     * Converte o DTO de requisição em entidade JPA.
     * A Empresa é recebida como parâmetro (já validada e buscada do banco).
     * Os campos de endereço são preenchidos depois via ViaCEP.
     */
    private Cliente toEntity(ClienteRequestDTO dto, Empresa empresa) {
        return Cliente.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .telefone(dto.getTelefone())
                .cep(dto.getCep())
                .empresa(empresa) // associa o cliente ao tenant correto
                .build();
    }
}
