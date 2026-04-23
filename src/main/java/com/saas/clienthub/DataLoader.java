package com.saas.clienthub;

import com.saas.clienthub.model.dto.EnderecoViaCepDTO;
import com.saas.clienthub.model.entity.Cliente;
import com.saas.clienthub.model.entity.Empresa;
import com.saas.clienthub.model.entity.Plano;
import com.saas.clienthub.model.entity.Role;
import com.saas.clienthub.model.entity.Tag;
import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.ClienteRepository;
import com.saas.clienthub.repository.EmpresaRepository;
import com.saas.clienthub.repository.TagRepository;
import com.saas.clienthub.repository.UsuarioRepository;
import com.saas.clienthub.service.ViaCepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Carrega dados de exemplo no banco na primeira vez que a aplicação sobe.
 *
 * =====================================================================
 * CONCEITO: CommandLineRunner
 * =====================================================================
 * A interface CommandLineRunner tem um único método: run(String... args).
 * O Spring Boot chama este método automaticamente APÓS o contexto da
 * aplicação estar totalmente inicializado (beans criados, banco conectado).
 *
 * É ideal para: inicialização de dados, verificações de saúde, migrações simples.
 *
 * =====================================================================
 * CONCEITO: @Component
 * =====================================================================
 * @Component é o estereótipo genérico do Spring.
 * Como implementamos CommandLineRunner, o Spring detecta esta classe,
 * a registra no contexto e chama run() na inicialização.
 *
 * =====================================================================
 * CONCEITO: Idempotência do DataLoader
 * =====================================================================
 * A primeira linha do run() verifica se já existem dados:
 *   if (empresaRepository.count() > 0) return;
 *
 * Isso torna o DataLoader idempotente — pode ser chamado N vezes
 * sem duplicar dados. Essencial para ambientes com restart frequente.
 *
 * =====================================================================
 * CONCEITO: Fallback de Dados
 * =====================================================================
 * Para cada cliente, tentamos buscar o endereço via ViaCEP.
 * Se a API estiver indisponível (sem internet, timeout, etc.),
 * usamos endereços hardcoded como fallback. Isso garante que os
 * dados de exemplo sempre sejam carregados corretamente.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final TagRepository tagRepository;
    private final UsuarioRepository usuarioRepository;
    private final ViaCepService viaCepService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Mapa de endereços hardcoded usados como fallback quando a API ViaCEP falha.
     * Chave: CEP (sem pontuação)
     * Valor: array com [logradouro, bairro, cidade, uf]
     *
     * Map.of() → cria um Map imutável (não pode ser alterado após a criação)
     */
    private static final Map<String, String[]> FALLBACK_ENDERECOS = Map.ofEntries(
            Map.entry("01001000", new String[]{"Praça da Sé", "Sé", "São Paulo", "SP"}),
            Map.entry("20040020", new String[]{"Rua do Ouvidor", "Centro", "Rio de Janeiro", "RJ"}),
            Map.entry("30130000", new String[]{"Rua da Bahia", "Centro", "Belo Horizonte", "MG"}),
            Map.entry("80020310", new String[]{"Rua XV de Novembro", "Centro", "Curitiba", "PR"}),
            Map.entry("40020000", new String[]{"Rua Chile", "Comércio", "Salvador", "BA"}),
            Map.entry("60060440", new String[]{"Rua Guilherme Rocha", "Centro", "Fortaleza", "CE"}),
            Map.entry("69005040", new String[]{"Rua José Clemente", "Centro", "Manaus", "AM"}),
            Map.entry("88010000", new String[]{"Rua Felipe Schmidt", "Centro", "Florianópolis", "SC"}),
            Map.entry("50010000", new String[]{"Avenida Marquês de Olinda", "Recife", "Recife", "PE"}),
            Map.entry("90010000", new String[]{"Rua dos Andradas", "Centro Histórico", "Porto Alegre", "RS"}),
            Map.entry("70040010", new String[]{"SBS Quadra 2", "Asa Sul", "Brasília", "DF"}),
            Map.entry("64000040", new String[]{"Rua Álvaro Mendes", "Centro", "Teresina", "PI"}),
            Map.entry("79002000", new String[]{"Rua 14 de Julho", "Centro", "Campo Grande", "MS"}),
            Map.entry("57020000", new String[]{"Rua do Comércio", "Centro", "Maceió", "AL"}),
            Map.entry("65010000", new String[]{"Rua Grande", "Centro", "São Luís", "MA"})
    );

    public DataLoader(EmpresaRepository empresaRepository,
                      ClienteRepository clienteRepository,
                      TagRepository tagRepository,
                      UsuarioRepository usuarioRepository,
                      ViaCepService viaCepService,
                      PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.tagRepository = tagRepository;
        this.usuarioRepository = usuarioRepository;
        this.viaCepService = viaCepService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Método executado automaticamente pelo Spring Boot na inicialização.
     * @param args argumentos de linha de comando (não usamos aqui)
     */
    @Override
    public void run(String... args) {
        // Idempotência: só carrega empresas/clientes se o banco estiver vazio
        boolean empresasExistem = empresaRepository.count() > 0;

        if (empresasExistem && usuarioRepository.count() > 0) {
            // Empresas e usuários já existem — nada a fazer
            return;
        }

        log.info("Carregando dados de exemplo...");

        // Variáveis para as empresas — podem vir do banco ou ser criadas agora
        Empresa techNova;
        Empresa cafeArte;
        Empresa startupZero;

        if (!empresasExistem) {
            // ============================================================
            // Empresa 1: TechNova Solutions — Plano ENTERPRISE
            // ============================================================
            techNova = empresaRepository.save(Empresa.builder()
                    .nome("TechNova Solutions")
                    .cnpj("11222333000181")
                    .email("contato@technova.com.br")
                    .plano(Plano.ENTERPRISE)
                    .build());

            criarCliente(techNova, "Ana Silva", "ana.silva@email.com", "(11) 98765-4321", "01001000");
            criarCliente(techNova, "Carlos Oliveira", "carlos.oliveira@email.com", "(21) 97654-3210", "20040020");
            criarCliente(techNova, "Marina Santos", "marina.santos@email.com", "(31) 96543-2109", "30130000");
            criarCliente(techNova, "Pedro Costa", "pedro.costa@email.com", "(41) 95432-1098", "80020310");
            criarCliente(techNova, "Fernanda Almeida", "fernanda.almeida@email.com", "(51) 94321-0987", "90010000");
            criarCliente(techNova, "Roberto Dias", "roberto.dias@email.com", "(61) 93210-9876", "70040010");
            criarCliente(techNova, "Juliana Martins", "juliana.martins@email.com", "(86) 92109-8765", "64000040");
            criarCliente(techNova, "Thiago Pereira", "thiago.pereira@email.com", "(67) 91098-7654", "79002000");

            // ============================================================
            // Empresa 2: Café & Arte Ltda — Plano PROFISSIONAL
            // ============================================================
            cafeArte = empresaRepository.save(Empresa.builder()
                    .nome("Café & Arte Ltda")
                    .cnpj("44555666000199")
                    .email("contato@cafearte.com.br")
                    .plano(Plano.PROFISSIONAL)
                    .build());

            criarCliente(cafeArte, "Julia Ferreira", "julia.ferreira@email.com", "(71) 94321-0987", "40020000");
            criarCliente(cafeArte, "Rafael Souza", "rafael.souza@email.com", "(85) 93210-9876", "60060440");
            criarCliente(cafeArte, "Beatriz Lima", "beatriz.lima@email.com", "(92) 92109-8765", "69005040");
            criarCliente(cafeArte, "Diego Nascimento", "diego.nascimento@email.com", "(82) 91098-7654", "57020000");
            criarCliente(cafeArte, "Larissa Carvalho", "larissa.carvalho@email.com", "(98) 90987-6543", "65010000");
            criarCliente(cafeArte, "Marcos Ribeiro", "marcos.ribeiro@email.com", "(11) 89876-5432", "01001000");

            // ============================================================
            // Empresa 3: StartUp Zero — Plano BASICO
            // ============================================================
            startupZero = empresaRepository.save(Empresa.builder()
                    .nome("StartUp Zero")
                    .cnpj("77888999000155")
                    .email("hello@startupzero.io")
                    .plano(Plano.BASICO)
                    .build());

            criarCliente(startupZero, "Lucas Mendes", "lucas.mendes@email.com", "(48) 91098-7654", "88010000");
            criarCliente(startupZero, "Camila Rocha", "camila.rocha@email.com", "(81) 90987-6543", "50010000");

            // ============================================================
            // Criação de Tags por empresa
            // ============================================================

            // TechNova tags
            Tag tnVip = criarTag(techNova, "VIP", "#FF9500");
            Tag tnNovo = criarTag(techNova, "Novo", "#34C759");
            Tag tnSudeste = criarTag(techNova, "Região Sudeste", "#007AFF");
            Tag tnInativo = criarTag(techNova, "Inativo", "#8E8E93");

            // Café & Arte tags
            Tag caFiel = criarTag(cafeArte, "Cliente Fiel", "#AF52DE");
            Tag caNovo = criarTag(cafeArte, "Novo", "#34C759");
            Tag caAtacado = criarTag(cafeArte, "Atacado", "#FF3B30");

            // StartUp Zero tags
            Tag szEarly = criarTag(startupZero, "Early Adopter", "#FF9500");
            Tag szBeta = criarTag(startupZero, "Beta Tester", "#5856D6");

            // ============================================================
            // Associação Tags → Clientes
            // ============================================================

            // TechNova: Ana Silva → VIP + Região Sudeste
            associarTags("ana.silva@email.com", techNova, Set.of(tnVip, tnSudeste));
            // TechNova: Carlos Oliveira → Novo + Região Sudeste
            associarTags("carlos.oliveira@email.com", techNova, Set.of(tnNovo, tnSudeste));
            // TechNova: Marina Santos → VIP
            associarTags("marina.santos@email.com", techNova, Set.of(tnVip));
            // TechNova: Pedro Costa → Inativo
            associarTags("pedro.costa@email.com", techNova, Set.of(tnInativo));
            // TechNova: Fernanda Almeida → VIP + Novo
            associarTags("fernanda.almeida@email.com", techNova, Set.of(tnVip, tnNovo));
            // TechNova: Roberto Dias → Novo
            associarTags("roberto.dias@email.com", techNova, Set.of(tnNovo));
            // TechNova: Juliana Martins → Região Sudeste
            associarTags("juliana.martins@email.com", techNova, Set.of(tnSudeste));
            // TechNova: Thiago Pereira → VIP
            associarTags("thiago.pereira@email.com", techNova, Set.of(tnVip));

            // Café & Arte: Julia Ferreira → Cliente Fiel
            associarTags("julia.ferreira@email.com", cafeArte, Set.of(caFiel));
            // Café & Arte: Rafael Souza → Novo + Atacado
            associarTags("rafael.souza@email.com", cafeArte, Set.of(caNovo, caAtacado));
            // Café & Arte: Beatriz Lima → Cliente Fiel + Atacado
            associarTags("beatriz.lima@email.com", cafeArte, Set.of(caFiel, caAtacado));
            // Café & Arte: Diego Nascimento → Novo
            associarTags("diego.nascimento@email.com", cafeArte, Set.of(caNovo));
            // Café & Arte: Larissa Carvalho → Cliente Fiel
            associarTags("larissa.carvalho@email.com", cafeArte, Set.of(caFiel));
            // Café & Arte: Marcos Ribeiro → Atacado
            associarTags("marcos.ribeiro@email.com", cafeArte, Set.of(caAtacado));

            // StartUp Zero: Lucas Mendes → Early Adopter + Beta Tester
            associarTags("lucas.mendes@email.com", startupZero, Set.of(szEarly, szBeta));
            // StartUp Zero: Camila Rocha → Early Adopter
            associarTags("camila.rocha@email.com", startupZero, Set.of(szEarly));

        } else {
            // Empresas já existem no banco — busca por CNPJ para vincular aos usuários
            techNova = empresaRepository.findByCnpj("11222333000181").orElse(null);
            cafeArte = empresaRepository.findByCnpj("44555666000199").orElse(null);
            startupZero = empresaRepository.findByCnpj("77888999000155").orElse(null);
            log.info("Empresas já existem no banco. Criando apenas os usuários...");
        }

        // ============================================================
        // Criação de Usuários de exemplo (se ainda não existirem)
        // ============================================================
        // Todas as senhas são criptografadas com BCrypt antes de salvar.
        // NUNCA armazene senhas em texto puro no banco de dados.
        if (usuarioRepository.count() == 0) {
            // 1. ADMIN — acesso global, sem empresa vinculada
            usuarioRepository.save(Usuario.builder()
                    .nome("Administrador")
                    .email("admin@clienthub.com")
                    .senha(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .empresa(null) // ADMIN não pertence a nenhuma empresa
                    .build());

            // 2. GESTOR da TechNova Solutions
            if (techNova != null) {
                usuarioRepository.save(Usuario.builder()
                        .nome("João Silva")
                        .email("joao@technova.com")
                        .senha(passwordEncoder.encode("gestor123"))
                        .role(Role.GESTOR)
                        .empresa(techNova)
                        .build());
            }

            // 3. GESTOR do Café & Arte
            if (cafeArte != null) {
                usuarioRepository.save(Usuario.builder()
                        .nome("Maria Souza")
                        .email("maria@cafearte.com")
                        .senha(passwordEncoder.encode("gestor123"))
                        .role(Role.GESTOR)
                        .empresa(cafeArte)
                        .build());
            }

            // 4. GESTOR da StartUp Zero
            if (startupZero != null) {
                usuarioRepository.save(Usuario.builder()
                        .nome("Ana Costa")
                        .email("ana@startupzero.com")
                        .senha(passwordEncoder.encode("gestor123"))
                        .role(Role.GESTOR)
                        .empresa(startupZero)
                        .build());
            }

            // 5. USUARIO da TechNova (acesso básico, apenas visualização)
            if (techNova != null) {
                usuarioRepository.save(Usuario.builder()
                        .nome("Pedro Santos")
                        .email("pedro@technova.com")
                        .senha(passwordEncoder.encode("user123"))
                        .role(Role.USUARIO)
                        .empresa(techNova)
                        .build());
            }

            log.info("Usuários de exemplo criados. Login admin: admin@clienthub.com / admin123");
        }

        // Loga o resumo dos dados carregados
        long totalEmpresas = empresaRepository.count();
        long totalClientes = clienteRepository.count();
        long totalUsuarios = usuarioRepository.count();
        log.info("Dados de exemplo carregados: {} empresas, {} clientes, {} usuários", totalEmpresas, totalClientes, totalUsuarios);
    }

    /**
     * Cria e persiste um cliente, tentando preencher o endereço via ViaCEP.
     * Se a API falhar, usa os dados do mapa FALLBACK_ENDERECOS.
     *
     * Cada chamada ao ViaCEP é isolada em seu próprio try-catch —
     * se um CEP falhar, os outros clientes continuam sendo criados normalmente.
     *
     * @param empresa  tenant ao qual o cliente pertence
     * @param nome     nome completo do cliente
     * @param email    email único dentro da empresa
     * @param telefone telefone formatado
     * @param cep      CEP sem pontuação (8 dígitos)
     */
    private void criarCliente(Empresa empresa, String nome, String email, String telefone, String cep) {
        // Começa construindo o cliente sem endereço
        Cliente cliente = Cliente.builder()
                .nome(nome)
                .email(email)
                .telefone(telefone)
                .cep(cep)
                .empresa(empresa) // vincula ao tenant correto
                .build();

        try {
            // Tenta buscar o endereço real via API ViaCEP
            EnderecoViaCepDTO endereco = viaCepService.buscarCep(cep);
            cliente.setLogradouro(endereco.getLogradouro());
            cliente.setBairro(endereco.getBairro());
            cliente.setCidade(endereco.getLocalidade()); // ViaCEP usa "localidade" para o nome da cidade
            cliente.setUf(endereco.getUf());
        } catch (Exception e) {
            // Se falhar (sem internet, CEP inválido, timeout), usa o fallback hardcoded
            log.warn("Falha ao buscar CEP {} via ViaCEP, usando fallback: {}", cep, e.getMessage());
            String[] fallback = FALLBACK_ENDERECOS.get(cep);
            if (fallback != null) {
                cliente.setLogradouro(fallback[0]);
                cliente.setBairro(fallback[1]);
                cliente.setCidade(fallback[2]);
                cliente.setUf(fallback[3]);
            }
        }

        // Persiste o cliente no banco (com ou sem endereço)
        clienteRepository.save(cliente);
    }

    /**
     * Cria e persiste uma tag vinculada a uma empresa.
     */
    private Tag criarTag(Empresa empresa, String nome, String cor) {
        return tagRepository.save(Tag.builder()
                .nome(nome)
                .cor(cor)
                .empresa(empresa)
                .build());
    }

    /**
     * Associa um conjunto de tags a um cliente existente (busca por email + empresa).
     */
    private void associarTags(String email, Empresa empresa, Set<Tag> tags) {
        List<Cliente> clientes = clienteRepository.findByEmpresaIdWithTags(empresa.getId());
        clientes.stream()
                .filter(c -> c.getEmail().equals(email))
                .findFirst()
                .ifPresent(cliente -> {
                    cliente.getTags().addAll(tags);
                    clienteRepository.save(cliente);
                });
    }
}
