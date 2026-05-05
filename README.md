# Cardápio Digital — Backend

Backend Java + Spring Boot do cardápio digital.

## Pré-requisitos

- JDK 21
- Docker (PostgreSQL local via docker-compose)
- Maven 3.9+ (ou usar `./mvnw`)

## Rodando

```bash
docker compose up -d            # sobe Postgres
./mvnw spring-boot:run          # inicia o app na porta 8080
```

Healthcheck: http://localhost:8080/actuator/health

## Testes

```bash
./mvnw test                      # roda testes (Testcontainers sobe Postgres efêmero)
```
