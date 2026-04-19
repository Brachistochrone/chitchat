# Chitchat

A real-time web chat application built with Java 21, Spring Boot 4, and React 19.

## Tech Stack

**Backend:**
- Spring Boot 4.0.5, Spring Security, Spring Data JPA, Spring for GraphQL
- PostgreSQL 18 with Flyway migrations
- Apache Kafka + Kafka Streams
- WebSocket (STOMP) + GraphQL Subscriptions
- Caffeine cache
- SpringDoc OpenAPI (Swagger), GraphiQL

**Frontend:**
- React 19, TypeScript, Vite
- React Router v7, Zustand (state management)
- Axios (HTTP), STOMP.js + SockJS (WebSocket)
- Tailwind CSS, Lucide icons, emoji-mart

## Prerequisites

- Java 21
- Node.js 20+ and npm
- Docker & Docker Compose
- Maven 3.9+

---

## Quick Start (Docker)

The fastest way to run the full stack — backend, database, Kafka, and migrations all in Docker:

```bash
docker compose up --build
```

The backend starts at `http://localhost:8080`.

To run the frontend dev server alongside:

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev
```

Open `http://localhost:5173`. The Vite dev server proxies API calls to the backend at `:8080`.

---

## Development Setup (without Docker for the app)

### 1. Start infrastructure

PostgreSQL and Kafka are required. Start them via Docker:

```bash
docker compose up postgres kafka zookeeper flyway -d
```

This starts the database, Kafka broker, and runs Flyway migrations.

### 2. Run the backend

```bash
export JAVA_HOME=/path/to/jdk-21
mvn spring-boot:run
```

The backend starts at `http://localhost:8080`.

### 3. Run the frontend

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev
```

Open `http://localhost:5173`. The Vite dev server proxies `/api` and `/ws` requests to `http://localhost:8080`.

---

## Build

### Backend

```bash
mvn clean package
```

### Frontend

```bash
cd frontend
npm run build
```

Production output is in `frontend/dist/`. To serve it from Spring Boot, copy the contents to `src/main/resources/static/`.

### Docker image (backend only)

```bash
docker build -t chitchat .
```

---

## Test

### Backend tests (95 tests)

```bash
mvn test
```

### Frontend type check

```bash
cd frontend
npx tsc --noEmit
```

---

## API

| Interface | URL |
|---|---|
| Frontend (dev) | http://localhost:5173 |
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

---

## Project Structure

```
chitchat/
├── src/main/java/com/chitchat/app/
│   ├── configuration/     Spring configs (Security, Kafka, WebSocket, Cache, OpenAPI)
│   ├── rest/              REST controllers
│   ├── graphql/           GraphQL resolvers (query, mutation, subscription)
│   ├── websocket/         STOMP handlers and event listeners
│   ├── service/           Business logic (interface + impl)
│   ├── dao/               Spring Data JPA repositories
│   ├── entity/            JPA entities and enums
│   ├── dto/               Request and response DTOs
│   ├── kafka/             Kafka producers, consumers, Streams topology
│   ├── security/          JWT filter, token provider, UserDetailsService
│   ├── exception/         Custom exceptions and global handler
│   └── util/              Utility classes (EntityMapper, SecurityUtil, etc.)
├── src/main/resources/
│   ├── db/migration/      Flyway SQL migrations (V1–V11)
│   ├── graphql/           GraphQL schema
│   └── application.properties
├── frontend/
│   ├── src/
│   │   ├── api/           Axios HTTP client and API functions
│   │   ├── components/    React components (auth, chat, layout, room, contact, profile, ui)
│   │   ├── hooks/         Custom hooks (useWebSocket)
│   │   ├── pages/         Route-level pages (Landing, Chat)
│   │   ├── stores/        Zustand state stores
│   │   └── types/         TypeScript interfaces
│   ├── index.html
│   └── vite.config.ts
├── docker-compose.yml     PostgreSQL, Kafka, Zookeeper, Flyway, App
├── Dockerfile             Multi-stage build (Maven → Alpine JRE)
└── pom.xml
```

---

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
