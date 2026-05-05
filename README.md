# Cardápio Digital — Backend

Backend Java + Spring Boot do cardápio digital.

## Pré-requisitos

- JDK 21
- Docker (PostgreSQL local via docker-compose)
- Maven 3.9+ (ou usar `./mvnw`)

## Rodando

> **Nota:** instruções completas (incluindo `docker-compose.yml` para PostgreSQL local) serão adicionadas conforme as próximas tasks da Fase 1.A. Por enquanto o esqueleto apenas compila e expõe `/actuator/health` sem persistência.

```bash
./mvnw -DskipTests package      # build do esqueleto
```

Healthcheck (após `./mvnw spring-boot:run`): http://localhost:8080/actuator/health
