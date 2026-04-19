# TODO — Milestone 1: Project Foundation & Auth

---

## 1. Maven Project Setup

- [ ] Create standard Maven directory layout:
  `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`
- [ ] Write `pom.xml` with:
  - Parent: `spring-boot-starter-parent 4.0.5`
  - Java 21 compiler settings (`maven.compiler.source/target`)
  - Dependencies:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-security`
    - `spring-boot-starter-validation`
    - `spring-boot-starter-websocket`
    - `spring-boot-starter-webflux`
    - `spring-kafka`
    - `spring-graphql`
    - `springdoc-openapi-starter-webmvc-ui`
    - `postgresql` (runtime)
    - `flyway-core`, `flyway-database-postgresql`
    - `lombok`
    - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (JWT)
    - `caffeine`, `spring-boot-starter-cache`
    - `spring-boot-starter-mail` (password reset emails)
    - `spring-boot-starter-test`, `spring-security-test` (test scope)
  - Maven wrapper (`mvnw`) configured
- [ ] Verify `mvn clean package -DskipTests` completes successfully

---

## 2. Docker Compose & Infrastructure

- [ ] Write `docker-compose.yml` with four services:
  - **zookeeper** (`confluentinc/cp-zookeeper:7.x`) — port 2181
  - **kafka** (`confluentinc/cp-kafka:7.x`) — port 9092; depends on zookeeper; env `KAFKA_ADVERTISED_LISTENERS`, `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`
  - **postgres** (`postgres:18`) — port 5432; env `POSTGRES_DB=chitchat`, `POSTGRES_USER`, `POSTGRES_PASSWORD`; named volume for data persistence; health check
  - **app** — builds from `Dockerfile`; depends on postgres (healthy) and kafka; env vars for DB URL, Kafka bootstrap, JWT secret; port 8080
- [ ] Write multi-stage `Dockerfile`:
  - Stage 1 (`maven:3.9-eclipse-temurin-21`): copies `pom.xml` + `src/`, runs `mvn package -DskipTests`, produces fat JAR
  - Stage 2 (`eclipse-temurin:21-jre-alpine`): copies JAR from stage 1, `ENTRYPOINT ["java", "-jar", "app.jar"]`
- [ ] Write `src/main/resources/application.properties`:
  - `spring.datasource.*` (URL, username, password via env vars)
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.flyway.enabled=true`
  - `spring.kafka.bootstrap-servers` (via env var)
  - `app.jwt.secret` and `app.jwt.expiration-ms` (via env var)
  - `app.mail.*` (SMTP settings)
  - `server.port=8080`
- [ ] Verify `docker compose up` starts all four containers without errors

---

## 3. Flyway Migrations (Full Schema)

All migrations in `src/main/resources/db/migration/`.

- [ ] `V1__create_users.sql` — `users` table:
  `id BIGSERIAL PK`, `email VARCHAR(255) UNIQUE NOT NULL`, `username VARCHAR(50) UNIQUE NOT NULL`, `display_name VARCHAR(100)`, `password_hash VARCHAR(255) NOT NULL`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `deleted_at TIMESTAMPTZ`

- [ ] `V2__create_user_sessions.sql` — `user_sessions` table:
  `id BIGSERIAL PK`, `user_id BIGINT FK→users(id) ON DELETE CASCADE`, `token_hash VARCHAR(255) UNIQUE NOT NULL`, `browser VARCHAR(255)`, `ip_address VARCHAR(45)`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `revoked BOOLEAN NOT NULL DEFAULT FALSE`

- [ ] `V3__create_rooms.sql` — `rooms` table:
  `id BIGSERIAL PK`, `name VARCHAR(100) UNIQUE NOT NULL`, `description TEXT`, `visibility VARCHAR(10) NOT NULL CHECK IN ('PUBLIC','PRIVATE')`, `owner_id BIGINT FK→users(id)`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`

- [ ] `V4__create_room_members.sql` — `room_members` table:
  composite PK `(room_id, user_id)`, `role VARCHAR(10) NOT NULL CHECK IN ('OWNER','ADMIN','MEMBER')`, `joined_at TIMESTAMPTZ NOT NULL DEFAULT now()`

- [ ] `V5__create_room_bans.sql` — `room_bans` table:
  composite PK `(room_id, user_id)`, `banned_by BIGINT FK→users(id)`, `banned_at TIMESTAMPTZ NOT NULL DEFAULT now()`

- [ ] `V6__create_room_invites.sql` — `room_invites` table:
  `id BIGSERIAL PK`, `room_id FK→rooms(id) ON DELETE CASCADE`, `invited_user_id FK→users(id) ON DELETE CASCADE`, `invited_by FK→users(id)`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, UNIQUE `(room_id, invited_user_id)`

- [ ] `V7__create_contacts.sql` — `contacts` table:
  `id BIGSERIAL PK`, `requester_id FK→users(id) ON DELETE CASCADE`, `addressee_id FK→users(id) ON DELETE CASCADE`, `status VARCHAR(10) NOT NULL CHECK IN ('PENDING','ACCEPTED')`, `message VARCHAR(255)`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`, UNIQUE `(requester_id, addressee_id)`

- [ ] `V8__create_user_bans.sql` — `user_bans` table:
  `id BIGSERIAL PK`, `banner_id FK→users(id) ON DELETE CASCADE`, `banned_id FK→users(id) ON DELETE CASCADE`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, UNIQUE `(banner_id, banned_id)`

- [ ] `V9__create_messages.sql` — `messages` table:
  `id BIGSERIAL PK`, `chat_type VARCHAR(10) NOT NULL CHECK IN ('ROOM','PERSONAL')`, `room_id FK→rooms(id) ON DELETE SET NULL`, `sender_id FK→users(id) NOT NULL`, `recipient_id FK→users(id)`, `content VARCHAR(3072)`, `reply_to_id FK→messages(id)`, `edited_at TIMESTAMPTZ`, `deleted_at TIMESTAMPTZ`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`;
  CHECK constraint ensuring ROOM↔room_id and PERSONAL↔recipient_id exclusivity;
  indexes: `idx_messages_room (room_id, created_at DESC) WHERE deleted_at IS NULL`,
  `idx_messages_personal (sender_id, recipient_id, created_at DESC) WHERE deleted_at IS NULL`

- [ ] `V10__create_attachments.sql` — `attachments` table:
  `id BIGSERIAL PK`, `message_id FK→messages(id) ON DELETE SET NULL`, `uploader_id FK→users(id) NOT NULL`, `original_filename VARCHAR(255) NOT NULL`, `stored_path VARCHAR(512) NOT NULL`, `file_size BIGINT NOT NULL`, `mime_type VARCHAR(127)`, `comment VARCHAR(500)`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`

- [ ] Verify Flyway applies all 10 migrations cleanly on a fresh DB (`mvn flyway:migrate` or app startup)

---

## 4. Entity Classes

Package: `com.chitchat.app.entity`

- [ ] `User.java` — fields matching `users` table; `@Table(name="users")`; Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`; `@Slf4j` where needed
- [ ] `UserSession.java` — fields matching `user_sessions`; `@ManyToOne` to `User`
- [ ] `Room.java` — fields matching `rooms`; `@ManyToOne owner`
- [ ] `RoomMember.java` — composite PK via `@EmbeddedId RoomMemberId`; `role` as `@Enumerated(EnumType.STRING)`
- [ ] `RoomBan.java` — composite PK via `@EmbeddedId`
- [ ] `RoomInvite.java`
- [ ] `Contact.java` — `status` as `@Enumerated(EnumType.STRING)`
- [ ] `UserBan.java`
- [ ] `Message.java` — `chatType` as enum; self-referential `@ManyToOne replyTo`
- [ ] `Attachment.java` — `@ManyToOne message`
- [ ] Enums: `RoomVisibility`, `RoomRole`, `ContactStatus`, `ChatType` in `entity/enums/`

---

## 5. Repository Interfaces

Package: `com.chitchat.app.dao`

- [ ] `UserRepository extends JpaRepository<User, Long>`:
  - `Optional<User> findByEmail(String email)`
  - `Optional<User> findByUsername(String username)`
  - `boolean existsByEmail(String email)`
  - `boolean existsByUsername(String username)`
- [ ] `UserSessionRepository extends JpaRepository<UserSession, Long>`:
  - `Optional<UserSession> findByTokenHashAndRevokedFalse(String tokenHash)`
  - `List<UserSession> findByUserIdAndRevokedFalse(Long userId)`
- [ ] `RoomRepository extends JpaRepository<Room, Long>`:
  - `Optional<Room> findByName(String name)`
  - `boolean existsByName(String name)`
  - `Page<Room> findByVisibilityAndNameContainingIgnoreCase(RoomVisibility visibility, String query, Pageable pageable)`
- [ ] `RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId>`
- [ ] `RoomBanRepository extends JpaRepository<RoomBan, RoomBanId>`
- [ ] `RoomInviteRepository extends JpaRepository<RoomInvite, Long>`
- [ ] `ContactRepository extends JpaRepository<Contact, Long>`
- [ ] `UserBanRepository extends JpaRepository<UserBan, Long>`
- [ ] `MessageRepository extends JpaRepository<Message, Long>`
- [ ] `AttachmentRepository extends JpaRepository<Attachment, Long>`

---

## 6. Security — JWT & Spring Security

Package: `com.chitchat.app.security`

- [ ] `JwtTokenProvider.java`:
  - `String generateToken(Long userId)` — signs with HS256, embeds `userId` as subject, sets expiry from `app.jwt.expiration-ms`
  - `Long getUserIdFromToken(String token)` — parses and validates; throws on expired/invalid
  - `boolean validateToken(String token)`
- [ ] `JwtAuthenticationFilter.java` extends `OncePerRequestFilter`:
  - Extracts `Authorization: Bearer <token>` header
  - Calls `JwtTokenProvider.validateToken`, loads `UserDetails`, sets `SecurityContextHolder`
  - Skips `/api/auth/**` paths
- [ ] `UserDetailsServiceImpl.java` implements `UserDetailsService`:
  - `loadUserByUsername(String userId)` — loads `User` by ID from `UserRepository`
  - Returns `org.springframework.security.core.userdetails.User` with roles
- [ ] `SecurityConfig.java` (`@Configuration`, `@EnableWebSecurity`):
  - Disable CSRF (stateless JWT)
  - Permit `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
  - All other requests require authentication
  - Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
  - Expose `AuthenticationManager` bean
  - `PasswordEncoder` bean: `BCryptPasswordEncoder(12)`

---

## 7. DTOs

Package: `com.chitchat.app.dto`

**Request DTOs** (`dto/request/`):
- [ ] `RegisterRequest`: `email` (`@Email @NotBlank`), `password` (`@Size(min=8) @NotBlank`), `username` (`@Pattern([a-zA-Z0-9_]{3,50}) @NotBlank`)
- [ ] `LoginRequest`: `email` (`@Email @NotBlank`), `password` (`@NotBlank`)
- [ ] `PasswordResetRequestDto`: `email` (`@Email @NotBlank`)
- [ ] `PasswordResetConfirmDto`: `token` (`@NotBlank`), `newPassword` (`@Size(min=8) @NotBlank`)
- [ ] `ChangePasswordRequest`: `currentPassword` (`@NotBlank`), `newPassword` (`@Size(min=8) @NotBlank`)
- [ ] `UpdateProfileRequest`: `displayName` (`@Size(max=100)`)

**Response DTOs** (`dto/response/`):
- [ ] `UserResponse`: `id`, `username`, `displayName`, `createdAt`
- [ ] `AuthResponse`: `token`, `user: UserResponse`
- [ ] `SessionResponse`: `id`, `browser`, `ipAddress`, `lastSeenAt`, `current` (boolean)
- [ ] `ErrorResponse`: `status`, `message`, `timestamp`

---

## 8. Service Layer

Package: `com.chitchat.app.service`

### AuthService / AuthServiceImpl
- [ ] `register(RegisterRequest)` → `UserResponse`:
  - Check email uniqueness → throw `ConflictException` if taken
  - Check username uniqueness → throw `ConflictException` if taken
  - Hash password with `BCryptPasswordEncoder`
  - Save `User` entity
  - Create `UserSession` (token = UUID, hash = SHA-256(token), store browser + IP)
  - Return `AuthResponse` with JWT + `UserResponse`
- [ ] `login(LoginRequest, HttpServletRequest)` → `AuthResponse`:
  - Find user by email → throw `UnauthorizedException` if not found or password mismatch
  - Create `UserSession` record
  - Return `AuthResponse`
- [ ] `logout(Long sessionId)`:
  - Mark `UserSession.revoked = true` for the given session
- [ ] `requestPasswordReset(String email)`:
  - Find user by email (no error if not found — silent for security)
  - Generate secure random token, store hashed token + expiry on user or dedicated table
  - Send reset email via `JavaMailSender`
- [ ] `confirmPasswordReset(PasswordResetConfirmDto)`:
  - Validate token (exists, not expired)
  - Hash new password, update `User.passwordHash`
  - Invalidate reset token
- [ ] `changePassword(Long userId, ChangePasswordRequest)`:
  - Verify current password matches
  - Hash and save new password
- [ ] `deleteAccount(Long userId)`:
  - Soft-delete or hard-delete user; cascade handled by DB ON DELETE rules

### UserService / UserServiceImpl
- [ ] `getMe(Long userId)` → `UserResponse`
- [ ] `updateProfile(Long userId, UpdateProfileRequest)` → `UserResponse`
- [ ] `getUserByUsername(String username)` → `UserResponse`

### SessionService / SessionServiceImpl
- [ ] `getActiveSessions(Long userId)` → `List<SessionResponse>`:
  - Query `UserSessionRepository` for non-revoked sessions
  - Mark the current session (`current=true`) by matching token hash from request context
- [ ] `revokeSession(Long userId, Long sessionId)`:
  - Verify session belongs to user → throw `AccessDeniedException` if not
  - Set `revoked = true`

---

## 9. Exception Handling

Package: `com.chitchat.app.exception`

- [ ] `ResourceNotFoundException` extends `RuntimeException` (maps to 404)
- [ ] `ConflictException` extends `RuntimeException` (maps to 409)
- [ ] `AccessDeniedException` extends `RuntimeException` (maps to 403)
- [ ] `UnauthorizedException` extends `RuntimeException` (maps to 401)
- [ ] `ValidationException` extends `RuntimeException` (maps to 400)
- [ ] `GlobalExceptionHandler.java` (`@RestControllerAdvice`):
  - Handle each custom exception → return `ErrorResponse` with correct HTTP status
  - Handle `MethodArgumentNotValidException` → 400 with field errors
  - Handle generic `Exception` → 500

---

## 10. REST Controllers

Package: `com.chitchat.app.rest`

### AuthController (`/api/auth`)
- [ ] `POST /register` → calls `AuthService.register`; returns 201 + `AuthResponse`
- [ ] `POST /login` → calls `AuthService.login`; returns 200 + `AuthResponse`
- [ ] `POST /logout` → calls `AuthService.logout` with current session ID from JWT; returns 204
- [ ] `POST /password-reset/request` → calls `AuthService.requestPasswordReset`; returns 204
- [ ] `POST /password-reset/confirm` → calls `AuthService.confirmPasswordReset`; returns 204

### UserController (`/api/users`)
- [ ] `GET /me` → calls `UserService.getMe`; returns 200 + `UserResponse`
- [ ] `PUT /me` → calls `UserService.updateProfile`; returns 200 + `UserResponse`
- [ ] `PUT /me/password` → calls `AuthService.changePassword`; returns 204
- [ ] `DELETE /me` → calls `AuthService.deleteAccount`; returns 204
- [ ] `GET /{username}` → calls `UserService.getUserByUsername`; returns 200 + `UserResponse`

### SessionController (`/api/sessions`)
- [ ] `GET /` → calls `SessionService.getActiveSessions`; returns 200 + `List<SessionResponse>`
- [ ] `DELETE /{sessionId}` → calls `SessionService.revokeSession`; returns 204

---

## 11. OpenAPI / Swagger Configuration

- [ ] `OpenApiConfig.java`:
  - Set API title "Chitchat API", version "1.0", description
  - Add `SecurityScheme` for Bearer JWT (`securitySchemeName = "bearerAuth"`)
  - Apply security requirement globally (except auth endpoints)
- [ ] Annotate all controllers and DTOs with `@Operation`, `@ApiResponse`, `@Schema` where non-obvious
- [ ] Verify Swagger UI accessible at `http://localhost:8080/swagger-ui.html`

---

## 12. Unit Tests

Package: `com.chitchat.app.service` (test)

- [ ] `AuthServiceImplTest`:
  - `register_success` — valid input → user saved, JWT returned
  - `register_duplicateEmail` → `ConflictException` thrown
  - `register_duplicateUsername` → `ConflictException` thrown
  - `login_success` — correct credentials → `AuthResponse` returned
  - `login_wrongPassword` → `UnauthorizedException` thrown
  - `login_userNotFound` → `UnauthorizedException` thrown
  - `logout_success` — session marked revoked
  - `changePassword_success` — password updated
  - `changePassword_wrongCurrent` → exception thrown
  - `deleteAccount_success` — user deletion triggered
- [ ] `UserServiceImplTest`:
  - `getMe_success` → returns `UserResponse`
  - `getMe_notFound` → `ResourceNotFoundException`
  - `updateProfile_success` → updated fields returned
  - `getUserByUsername_success`
  - `getUserByUsername_notFound` → `ResourceNotFoundException`
- [ ] `SessionServiceImplTest`:
  - `getActiveSessions_returnsNonRevoked`
  - `revokeSession_success`
  - `revokeSession_wrongOwner` → `AccessDeniedException`
- [ ] All tests use Mockito (`@ExtendWith(MockitoExtension.class)`); no Spring context loaded

---

## 13. Smoke Test Checklist (Manual Verification)

- [ ] `docker compose up --build` — all 4 containers healthy
- [ ] `POST /api/auth/register` — 201 + JWT returned
- [ ] `POST /api/auth/login` — 200 + JWT returned
- [ ] `GET /api/users/me` with JWT — 200 + user object
- [ ] `GET /api/users/me` without JWT — 401
- [ ] `GET /api/sessions` — lists current session with `current: true`
- [ ] `DELETE /api/sessions/{id}` — session revoked; subsequent use of that JWT rejected (401)
- [ ] `POST /api/auth/password-reset/request` — 204 (no error even for unknown email)
- [ ] `DELETE /api/users/me` — 204; subsequent login with same credentials → 401
- [ ] Swagger UI loads at `http://localhost:8080/swagger-ui.html`
- [ ] `mvn test` — all unit tests pass
