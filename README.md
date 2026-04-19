# Chitchat

A real-time web chat application built with Java 21 and Spring Boot 4.

## Tech Stack

- **Backend**: Spring Boot 4.0.5, Spring Security, Spring Data JPA, Spring for GraphQL
- **Database**: PostgreSQL 18 with Flyway migrations
- **Messaging**: Apache Kafka + Kafka Streams
- **Real-time**: WebSocket (STOMP) + GraphQL Subscriptions
- **Cache**: Caffeine
- **API Docs**: SpringDoc OpenAPI (Swagger), GraphiQL
- **Build**: Maven, Docker

## Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.9+

## Build

```bash
mvn clean package
```

## Run

Start all services (PostgreSQL, Kafka, Flyway migrations, app):

```bash
docker compose up --build
```

The application starts at `http://localhost:8080`.

## API

| Interface | URL |
|---|---|
| REST API (Swagger) | http://localhost:8080/swagger-ui.html |
| GraphQL (GraphiQL) | http://localhost:8080/graphiql |
| WebSocket (STOMP) | ws://localhost:8080/ws |
| GraphQL Subscriptions | ws://localhost:8080/graphql |

### Authentication

All endpoints (except auth and docs) require a JWT Bearer token in the `Authorization` header.

```
Authorization: Bearer <token>
```

Obtain a token via `POST /api/auth/register` or `POST /api/auth/login`.

## Test

```bash
mvn test
```

## Project Structure

```
src/main/java/com/chitchat/app/
├── configuration/     Spring configs (Security, Kafka, WebSocket, Cache, OpenAPI)
├── rest/              REST controllers
├── graphql/           GraphQL resolvers (query, mutation, subscription)
├── websocket/         STOMP handlers and event listeners
├── service/           Business logic (interface + impl)
├── dao/               Spring Data JPA repositories
├── entity/            JPA entities and enums
├── dto/               Request and response DTOs
├── kafka/             Kafka producers, consumers, Streams topology, event POJOs
├── security/          JWT filter, token provider, UserDetailsService
├── exception/         Custom exceptions and global handler
└── util/              Utility classes (EntityMapper, SecurityUtil, AppConstants, etc.)
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/chitchat` | Database JDBC URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `JWT_SECRET` | (dev default) | JWT signing secret (min 32 chars) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | JWT token TTL |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | — | SMTP username |
| `MAIL_PASSWORD` | — | SMTP password |
| `STORAGE_PATH` | `/tmp/chitchat/uploads` | File upload directory |
