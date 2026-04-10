# ClientHub — Plataforma SaaS de Gestão de Clientes

Sistema completo de gestão de clientes com isolamento multi-tenant, onde cada empresa (tenant) possui sua própria base de clientes. Inclui autenticação com Spring Security, controle de acesso por roles (ADMIN, GESTOR, USUARIO), interface web com design inspirado na Apple e API REST documentada com Swagger.

## Tecnologias Utilizadas

- **Java 21** + **Spring Boot 4.x**
- **Spring Security** — Autenticação e autorização (form login + BCrypt)
- **Spring Data JPA** + **PostgreSQL** (produção) / **H2** (fallback)
- **Thymeleaf** + **thymeleaf-extras-springsecurity6** — Templates com controle de acesso
- **SpringDoc OpenAPI 3.x** — Documentação Swagger da API
- **Lombok** — Redução de boilerplate
- **Bean Validation** — Validação de dados
- **RestTemplate** — Consumo da API ViaCEP
- **Docker Compose** — Infraestrutura completa (PostgreSQL + app)
- **CSS customizado** — Design system inspirado na Apple (sem frameworks)

## Pré-Requisitos

- Java 21+
- Maven 3.8+
- Docker e Docker Compose (opcional — necessário para PostgreSQL)

## Como Rodar

### Opção 1: Tudo no Docker (recomendado)

```bash
# macOS / Linux
docker compose up --build

# Windows
docker-compose up --build
```

Isso sobe o PostgreSQL e a aplicação juntos. Acesse `http://localhost:8080` quando aparecer `Started ClientHubApplication`.

Para rodar em background, adicione `-d`:
```bash
docker compose up --build -d
```

> **Nota:** Se precisar recriar o banco do zero (ex: após mudanças nas entidades), use:
> ```bash
> docker compose down -v && docker compose up --build
> ```

### Opção 2: Apenas o banco no Docker + app local

```bash
# macOS / Linux
docker compose up -d postgres

# Windows
docker-compose up -d postgres
```

Depois rode a aplicação:
```bash
./mvnw spring-boot:run
```

### Opção 3: Com PostgreSQL local (sem Docker)

1. Crie o banco de dados:
```sql
CREATE DATABASE clienthub;
```
2. Execute a aplicação:
```bash
./mvnw spring-boot:run
```

### Opção 4: Sem banco externo (H2 em arquivo)

```bash
./mvnw spring-boot:run -Dspring.profiles.active=h2
```

## URLs de Acesso

| Recurso | URL |
|---------|-----|
| Aplicação Web (Login) | http://localhost:8080/login |
| Dashboard | http://localhost:8080 |
| Swagger UI (API Docs) | http://localhost:8080/swagger-ui.html |
| Console H2 (se perfil h2) | http://localhost:8080/h2-console |

## Autenticação e Segurança

### Usuários de Exemplo

A aplicação cria automaticamente os seguintes usuários na primeira execução:

| Usuário | Email | Senha | Role | Empresa |
|---------|-------|-------|------|---------|
| Administrador | `admin@clienthub.com` | `admin123` | ADMIN | — (acesso global) |
| João Silva | `joao@technova.com` | `gestor123` | GESTOR | TechNova Solutions |
| Maria Souza | `maria@cafearte.com` | `gestor123` | GESTOR | Café & Arte Ltda |
| Ana Costa | `ana@startupzero.com` | `gestor123` | GESTOR | StartUp Zero |
| Pedro Santos | `pedro@technova.com` | `user123` | USUARIO | TechNova Solutions |

### Permissões por Role

| Funcionalidade | ADMIN | GESTOR | USUARIO |
|----------------|:-----:|:------:|:-------:|
| Dashboard global (todas empresas) | ✅ | ❌ | ❌ |
| Dashboard da empresa | — | ✅ | ✅ |
| Criar / Editar / Desativar empresas | ✅ | ❌ | ❌ |
| Visualizar empresa (própria) | ✅ | ✅ | ✅ |
| Criar / Editar / Desativar clientes | ✅ | ✅ | ✅ |
| Gerenciar usuários (CRUD) | ✅ | ❌ | ❌ |
| Swagger / API Docs (navbar) | ✅ | ❌ | ❌ |
| API REST (`/api/**`) | ✅ | ✅ | ✅ |

### Isolamento Multi-Tenant

- **ADMIN** pode ver e gerenciar todas as empresas e seus clientes
- **GESTOR** e **USUARIO** só enxergam dados da própria empresa
- Se um GESTOR/USUARIO tentar acessar a URL de outra empresa, recebe erro de acesso negado
- A verificação ocorre na camada de **Service** (não apenas na interface), garantindo segurança mesmo via API

### Fluxo de Login (Web — Sessão)

1. Usuário acessa qualquer página → redirecionado para `/login`
2. Digita email e senha → Spring Security valida com BCrypt
3. Após login bem-sucedido:
   - **ADMIN** → redireciona para `/` (dashboard global)
   - **GESTOR/USUARIO** → redireciona para `/empresas/{id}` (detalhes da sua empresa)
4. Sessão expira após inatividade (configurável em `application.properties`)

### Autenticação da API REST (JWT)

A API REST (`/api/**`) é protegida com **JSON Web Token (JWT)**. O frontend web continua usando sessão/cookie normalmente.

**1. Obter o token:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@clienthub.com", "senha": "admin123"}'
```

Resposta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "email": "admin@clienthub.com",
  "nome": "Administrador",
  "role": "ADMIN"
}
```

**2. Usar o token nas requisições:**
```bash
curl http://localhost:8080/api/empresas \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**3. No Swagger UI**, clique em "Authorize" e insira: `Bearer <seu_token>`

| Aspecto | Web (Thymeleaf) | API REST |
|---------|:---------------:|:--------:|
| Autenticação | Sessão (cookie) | JWT (header) |
| Login | Form `/login` | POST `/api/auth/login` |
| Estado | Stateful (servidor guarda sessão) | Stateless (token auto-contido) |
| CSRF | Habilitado | Desabilitado |
| Expiração | Configurável (session timeout) | 24 horas (jwt.expiration) |

## Estrutura do Projeto

```
com.saas.clienthub/
├── ClientHubApplication.java
├── DataLoader.java                         # Dados de exemplo (empresas, clientes, usuários)
├── config/
│   ├── SecurityConfig.java                 # Dual auth: Sessão (web) + JWT (API)
│   ├── CustomAuthenticationSuccessHandler.java  # Redirect pós-login por role
│   ├── RestTemplateConfig.java             # RestTemplate com timeout
│   └── SwaggerConfig.java                  # Configuração OpenAPI
├── security/
│   ├── JwtService.java                     # Geração e validação de tokens JWT
│   └── JwtAuthenticationFilter.java        # Filtro que autentica via JWT
├── model/
│   ├── entity/                             # Entidades JPA
│   │   ├── Empresa.java
│   │   ├── Cliente.java
│   │   ├── Usuario.java                    # Usuário com role e empresa
│   │   ├── Plano.java                      # Enum: BASICO, PROFISSIONAL, ENTERPRISE
│   │   └── Role.java                       # Enum: ADMIN, GESTOR, USUARIO
│   └── dto/                                # DTOs de request/response
│       ├── EmpresaRequestDTO / ResponseDTO
│       ├── ClienteRequestDTO / ResponseDTO
│       ├── UsuarioRequestDTO / ResponseDTO
│       ├── LoginRequestDTO / ResponseDTO   # DTOs para autenticação JWT
│       ├── DashboardDTO
│       ├── EnderecoViaCepDTO
│       └── ErrorResponse
├── repository/                             # Spring Data JPA repositories
│   ├── EmpresaRepository.java
│   ├── ClienteRepository.java
│   └── UsuarioRepository.java
├── service/                                # Lógica de negócio
│   ├── EmpresaService.java                 # + verificação de tenant
│   ├── ClienteService.java                 # + verificação de tenant
│   ├── UsuarioService.java                 # CRUD + getUsuarioLogado()
│   ├── CustomUserDetailsService.java       # Integração Spring Security
│   └── ViaCepService.java
├── controller/
│   ├── rest/                               # API REST (JSON) — Swagger
│   │   ├── AuthRestController.java         # Login JWT (POST /api/auth/login)
│   │   ├── EmpresaRestController.java
│   │   ├── ClienteRestController.java
│   │   ├── UsuarioRestController.java
│   │   └── CepRestController.java
│   └── web/                                # Controllers Thymeleaf (HTML)
│       ├── DashboardController.java
│       ├── EmpresaWebController.java
│       ├── ClienteWebController.java
│       ├── UsuarioWebController.java
│       └── LoginController.java
└── exception/                              # Tratamento global de exceções
```

## Endpoints da API

> Todos os endpoints (exceto login) exigem autenticação JWT.
> Envie o header `Authorization: Bearer <token>` em cada requisição.

### Autenticação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | /api/auth/login | Login — retorna token JWT |

### Empresas

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/empresas | Listar todas as empresas |
| GET | /api/empresas/{id} | Buscar empresa por ID |
| POST | /api/empresas | Criar nova empresa |
| PUT | /api/empresas/{id} | Atualizar empresa |
| DELETE | /api/empresas/{id} | Desativar empresa |

### Clientes

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/empresas/{empresaId}/clientes | Listar clientes da empresa |
| GET | /api/empresas/{empresaId}/clientes/{id} | Buscar cliente por ID |
| GET | /api/empresas/{empresaId}/clientes/pesquisa?nome= | Pesquisar por nome |
| POST | /api/empresas/{empresaId}/clientes | Criar novo cliente |
| PUT | /api/empresas/{empresaId}/clientes/{id} | Atualizar cliente |
| DELETE | /api/empresas/{empresaId}/clientes/{id} | Desativar cliente |

### Usuários

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/usuarios | Listar todos os usuários |
| GET | /api/usuarios/{id} | Buscar usuário por ID |
| POST | /api/usuarios | Criar novo usuário |
| PUT | /api/usuarios/{id} | Atualizar usuário |
| DELETE | /api/usuarios/{id} | Desativar usuário (soft delete) |

### CEP

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/cep/{cep} | Consultar endereço por CEP |

> **Nota:** O endpoint `POST /api/auth/login` é público. Todos os demais endpoints da API exigem token JWT válido no header `Authorization: Bearer <token>`.

## Dados de Exemplo

A aplicação carrega automaticamente na primeira execução:

- **3 empresas**: TechNova Solutions (Enterprise), Café & Arte Ltda (Profissional), StartUp Zero (Basico)
- **9 clientes**: distribuídos entre as empresas, com endereços via ViaCEP
- **5 usuários**: 1 admin, 3 gestores (um por empresa), 1 usuário comum

Todas as senhas são armazenadas com hash BCrypt — nunca em texto puro.
