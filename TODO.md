# TODO — Milestone 6: GraphQL, Polish & Packaging

---

## 1. GraphQL Schema (`src/main/resources/graphql/schema.graphqls`)

- [x] Create `src/main/resources/graphql/` directory
- [x] Define full schema file with:
  - Scalars: `DateTime`
  - Enums: `RoomVisibility`, `RoomRole`, `PresenceStatus`, `ContactStatus`, `ChatType`, `EventType`
  - Types: `User`, `Room`, `RoomMember`, `Message`, `Attachment`, `Contact`, `Session`, `AuthPayload`, `MessagePage`, `RoomPage`
  - Subscription event types: `MessageEvent`, `PresenceEvent`, `NotificationEvent`
  - All Queries (§7 spec): `me`, `user`, `mySessions`, `rooms`, `room`, `roomMembers`, `roomBans`, `roomMessages`, `personalMessages`, `contacts`, `pendingRequests`
  - All Mutations (§7 spec): auth (7), sessions (1), rooms (10), messages (4), contacts (6)
  - All Subscriptions (§7 spec): `roomMessages`, `personalMessages`, `roomPresence`, `notifications`

---

## 2. GraphQL Configuration

### `application.properties` additions
- [x] `spring.graphql.graphiql.enabled=true` — enable GraphiQL playground
- [x] `spring.graphql.websocket.path=/graphql` — WebSocket transport for subscriptions
- [x] `spring.graphql.schema.printer.enabled=true` — introspection

### DateTime scalar configuration (`graphql/config/ScalarConfig.java`)
- [x] Register `ExtendedScalars.DateTime` from `graphql-java-extended-scalars` library
- [x] Add `graphql-java-extended-scalars` dependency to `pom.xml`
- [x] `@Bean RuntimeWiringConfigurer` to register the scalar

---

## 3. Query Resolvers (`graphql/query/`)

### `UserQueryResolver.java`
- [x] `@QueryMapping User me()` → delegates to `UserService.getMe`
- [x] `@QueryMapping User user(String username)` → delegates to `UserService.getUserByUsername`

### `SessionQueryResolver.java`
- [x] `@QueryMapping List<Session> mySessions()` → delegates to `SessionService.getActiveSessions`

### `RoomQueryResolver.java`
- [x] `@QueryMapping RoomPage rooms(String query, Integer page, Integer size)` → delegates to `RoomService.searchPublicRooms`
- [x] `@QueryMapping Room room(Long id)` → delegates to `RoomService.getRoom`
- [x] `@QueryMapping List<RoomMember> roomMembers(Long roomId)` → delegates to `RoomService.getMembers`
- [x] `@QueryMapping List<BanResponse> roomBans(Long roomId)` → delegates to `RoomService.getBans`

### `MessageQueryResolver.java`
- [x] `@QueryMapping MessagePage roomMessages(Long roomId, String before, Integer limit)` → delegates to `MessageService.getRoomMessages`; wraps result in `MessagePage` with `nextCursor`
- [x] `@QueryMapping MessagePage personalMessages(Long userId, String before, Integer limit)` → delegates to `MessageService.getPersonalMessages`; wraps result in `MessagePage`

### `ContactQueryResolver.java`
- [x] `@QueryMapping List<Contact> contacts()` → delegates to `ContactService.getFriends`
- [x] `@QueryMapping List<Contact> pendingRequests()` → delegates to `ContactService.getIncomingRequests`

---

## 4. Mutation Resolvers (`graphql/mutation/`)

### `AuthMutationResolver.java`
- [x] `@MutationMapping AuthPayload register(String email, String password, String username)` → `AuthService.register`
- [x] `@MutationMapping AuthPayload login(String email, String password)` → `AuthService.login`
- [x] `@MutationMapping Boolean logout()` → `AuthService.logout`
- [x] `@MutationMapping Boolean requestPasswordReset(String email)` → `AuthService.requestPasswordReset`
- [x] `@MutationMapping Boolean confirmPasswordReset(String token, String newPassword)` → `AuthService.confirmPasswordReset`
- [x] `@MutationMapping Boolean changePassword(String currentPassword, String newPassword)` → `AuthService.changePassword`
- [x] `@MutationMapping Boolean deleteAccount()` → `AuthService.deleteAccount`

### `RoomMutationResolver.java`
- [x] `@MutationMapping Room createRoom(...)` → `RoomService.createRoom`
- [x] `@MutationMapping Room updateRoom(...)` → `RoomService.updateRoom`
- [x] `@MutationMapping Boolean deleteRoom(Long id)` → `RoomService.deleteRoom`
- [x] `@MutationMapping Boolean joinRoom(Long id)` → `RoomService.joinRoom`
- [x] `@MutationMapping Boolean leaveRoom(Long id)` → `RoomService.leaveRoom`
- [x] `@MutationMapping Boolean inviteToRoom(Long roomId, String username)` → `RoomService.inviteUser`
- [x] `@MutationMapping Boolean promoteAdmin(Long roomId, Long userId)` → `RoomService.promoteAdmin`
- [x] `@MutationMapping Boolean demoteAdmin(Long roomId, Long userId)` → `RoomService.demoteAdmin`
- [x] `@MutationMapping Boolean kickMember(Long roomId, Long userId)` → `RoomService.kickMember`
- [x] `@MutationMapping Boolean banFromRoom(Long roomId, Long userId)` → `RoomService.banMember`
- [x] `@MutationMapping Boolean unbanFromRoom(Long roomId, Long userId)` → `RoomService.unbanMember`

### `MessageMutationResolver.java`
- [x] `@MutationMapping Message sendRoomMessage(Long roomId, String content, Long replyToId, List<Long> attachmentIds)` → `MessageService.sendRoomMessage`
- [x] `@MutationMapping Message sendPersonalMessage(Long userId, String content, Long replyToId, List<Long> attachmentIds)` → `MessageService.sendPersonalMessage`
- [x] `@MutationMapping Message editMessage(Long id, String content)` → `MessageService.editMessage`
- [x] `@MutationMapping Boolean deleteMessage(Long id)` → `MessageService.deleteMessage`

### `ContactMutationResolver.java`
- [x] `@MutationMapping Contact sendFriendRequest(String username, String message)` → `ContactService.sendFriendRequest`
- [x] `@MutationMapping Contact acceptFriendRequest(Long requestId)` → `ContactService.acceptFriendRequest`
- [x] `@MutationMapping Boolean declineFriendRequest(Long requestId)` → `ContactService.declineFriendRequest`
- [x] `@MutationMapping Boolean removeFriend(Long userId)` → `ContactService.removeFriend`
- [x] `@MutationMapping Boolean banUser(Long userId)` → `ContactService.banUser`
- [x] `@MutationMapping Boolean unbanUser(Long userId)` → `ContactService.unbanUser`

---

## 5. Subscription Resolvers (`graphql/subscription/`)

### `MessageSubscriptionResolver.java`
- [x] `@SubscriptionMapping Flux<MessageEvent> roomMessages(Long roomId)` — subscribe to room messages via `SimpMessagingTemplate` or a reactive `Sinks.Many<>`; backed by the Kafka consumer that fans out to WebSocket
- [x] `@SubscriptionMapping Flux<MessageEvent> personalMessages()` — subscribe to personal messages for the authenticated user

### `PresenceSubscriptionResolver.java`
- [x] `@SubscriptionMapping Flux<PresenceEvent> roomPresence(Long roomId)` — subscribe to presence changes for members of a room

### `NotificationSubscriptionResolver.java`
- [x] `@SubscriptionMapping Flux<NotificationEvent> notifications()` — subscribe to notifications for the authenticated user

### Subscription infrastructure
- [x] Create `graphql/subscription/SubscriptionPublisher.java` — holds `Sinks.Many<>` per topic (room messages, personal messages, presence, notifications); consumers publish to sinks, subscription resolvers subscribe from sinks
- [x] Update Kafka consumers (`MessageEventConsumer`, `PresenceStateConsumer`, `NotificationConsumer`) to also publish to `SubscriptionPublisher` sinks

---

## 6. GraphQL Security

- [x] Add `@Controller` annotation to all resolver classes (Spring GraphQL uses `@Controller` pattern)
- [x] Use `SecurityUtil.getCurrentUserId()` in resolvers for authenticated user context
- [x] Auth mutations (register, login, requestPasswordReset, confirmPasswordReset) should work without authentication — configure in `SecurityConfig`
- [x] Add `/graphql` and `/graphiql` paths to `SecurityConfig` permitAll (for GraphiQL playground)

### `SecurityConfig.java` updates
- [x] Add `"/graphql"`, `"/graphiql"` to permitted paths (GraphQL endpoint handles its own auth via resolvers)

### `AppConstants.java` updates
- [x] Add `GRAPHQL_PATH = "/graphql"` and `GRAPHIQL_PATH = "/graphiql"` constants

---

## 7. DTO Additions for GraphQL

### `dto/response/MessagePage.java`
- [x] `items` (`List<MessageResponse>`), `nextCursor` (`String`)

### `dto/response/RoomPage.java`
- [x] `items` (`List<RoomResponse>`), `totalCount` (`int`)

---

## 8. README.md

- [x] Create `README.md` at project root with:
  - Project description
  - Tech stack summary
  - Prerequisites (Java 21, Docker, Maven)
  - Build: `mvn clean package`
  - Run: `docker compose up --build`
  - REST API: `http://localhost:8080/swagger-ui.html`
  - GraphQL: `http://localhost:8080/graphiql`
  - Test: `mvn test`
  - Project structure overview
  - Environment variables table

---

## 9. Docker Polish

### Verify `Dockerfile`
- [x] Multi-stage build already in place ✓
- [x] Alpine runtime with `libstdc++` ✓

### Verify `docker-compose.yml`
- [x] Health checks on postgres ✓
- [x] Flyway runs before app ✓
- [x] Kafka topic auto-creation ✓
- [x] Ensure `app` container exposes all needed ports

---

## 10. Smoke Test Checklist (End-to-End)

### REST API flow
- [x] `POST /api/auth/register` → 201, user created
- [x] `POST /api/auth/login` → 200, JWT received
- [x] `POST /api/rooms` → 201, room created
- [x] `POST /api/rooms/{roomId}/messages` → 201, message sent
- [x] `POST /api/attachments` → 201, file uploaded
- [x] `GET /api/attachments/{id}` → 200, file downloaded
- [x] `GET /swagger-ui.html` → Swagger UI renders

### GraphQL flow
- [x] `mutation { register(...) }` → AuthPayload returned
- [x] `mutation { login(...) }` → token received
- [x] `query { me { username } }` → current user
- [x] `mutation { createRoom(...) }` → room created
- [x] `mutation { sendRoomMessage(...) }` → message sent
- [x] `query { roomMessages(roomId: ...) }` → message list
- [x] `subscription { roomMessages(roomId: ...) }` → real-time message received
- [x] `query { contacts }` → friend list
- [x] `GET /graphiql` → GraphiQL playground renders

### Docker flow
- [x] `docker compose up --build` → all services start
- [x] `docker compose logs flyway` → migrations applied
- [x] App connects to DB and Kafka without errors
- [x] `mvn test` — all tests pass
