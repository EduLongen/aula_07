# ClientHub — Plataforma SaaS Multi-Tenant de Gestão de Clientes

Sistema completo de gestão de clientes com isolamento multi-tenant, onde cada empresa (tenant) possui sua própria base de clientes. Interface web com design inspirado na Apple e API REST documentada com Swagger.

## Tecnologias Utilizadas

- **Java 21** + **Spring Boot 4.x**
- **Spring Data JPA** + **PostgreSQL** (produção) / **H2** (fallback)
- **Thymeleaf** — Template engine para interface web
- **SpringDoc OpenAPI 3.x** — Documentação Swagger da API
- **Lombok** — Redução de boilerplate
- **Bean Validation** — Validação de dados
- **RestTemplate** — Consumo da API ViaCEP
- **Docker Compose** — Infraestrutura PostgreSQL
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
| Aplicação Web | http://localhost:8080 |
| Swagger UI (API Docs) | http://localhost:8080/swagger-ui.html |
| Console H2 (se perfil h2) | http://localhost:8080/h2-console |

## Estrutura do Projeto

```
com.saas.clienthub/
├── ClientHubApplication.java
├── DataLoader.java                  # Dados de exemplo
├── config/
│   ├── RestTemplateConfig.java      # RestTemplate com timeout
│   └── SwaggerConfig.java           # Configuração OpenAPI
├── model/
│   ├── entity/                      # Entidades JPA
│   │   ├── Empresa.java
│   │   ├── Cliente.java
│   │   └── Plano.java
│   └── dto/                         # DTOs de request/response
├── repository/                      # Spring Data JPA repositories
├── service/                         # Lógica de negócio
│   ├── EmpresaService.java
│   ├── ClienteService.java
│   └── ViaCepService.java
├── controller/
│   ├── rest/                        # API REST (JSON)
│   └── web/                         # Controllers Thymeleaf (HTML)
└── exception/                       # Tratamento global de exceções
```

## Endpoints da API

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

### CEP

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/cep/{cep} | Consultar endereço por CEP |

## Dados de Exemplo

A aplicação carrega automaticamente 3 empresas e 9 clientes na primeira execução:

- **TechNova Solutions** (Enterprise) — 4 clientes
- **Cafe & Arte Ltda** (Profissional) — 3 clientes
- **StartUp Zero** (Basico) — 2 clientes
