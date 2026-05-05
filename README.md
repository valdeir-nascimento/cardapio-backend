# Cardápio Digital — Backend

Backend Java + Spring Boot do cardápio digital de uma pequena empresa (single-tenant).

Documentação:
- [Spec de design](docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md)
- [Plano fase 1.A — Foundation](docs/superpowers/plans/2026-05-04-phase-1a-foundation.md)

## Pré-requisitos

- JDK 21
- Docker (PostgreSQL local via docker-compose)
- Maven 3.9+ (ou usar `./mvnw`)

## Rodando localmente (perfil `dev`)

```bash
docker compose up -d                                   # sobe Postgres
./mvnw spring-boot:run                                 # default = dev
```

Healthcheck: http://localhost:8080/actuator/health

## Perfis

| Perfil | DB | Uso |
|---|---|---|
| `dev` | localhost via docker-compose | desenvolvimento |
| `test` | Testcontainers (efêmero) | testes automatizados |
| `staging` | env vars `DB_URL`, `DB_USER`, `DB_PASSWORD` | ambiente de homologação |
| `prod` | env vars + Hikari tunado | produção |

Trocar perfil:

```bash
SPRING_PROFILES_ACTIVE=staging ./mvnw spring-boot:run
# ou no jar
java -jar target/cardapio-backend-*.jar --spring.profiles.active=prod
```

## Testes

```bash
./mvnw test                                            # todos os testes
./mvnw test -Dtest=CleanArchitectureTest               # arquitetura
./mvnw test -Dtest=ModulithVerificationTest            # fronteiras de módulos
```

## Estrutura

Single-module Maven com **Spring Modulith** — cada bounded context vira um pacote Java:

```
com.cardapio
├── shared/            # kernel: Money, Email, CPF, Notification, Result
└── api/error/         # RFC 7807 problem handling
```

Próximas fases adicionarão `identity`, `catalog`, `delivery`, `ordering`, `payment`, `notification`, `promotion`, `review`.

## Convenções

- **Domínio puro:** `*.domain.*` não pode importar Spring/JPA (verificado por ArchUnit)
- **Casos de uso retornam `Result<T>`** com `Notification` para erros de negócio
- **Eventos de domínio** publicados via Spring Modulith (outbox transacional automático)
- **Erros HTTP** seguem RFC 7807 (`application/problem+json`)
