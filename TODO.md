# TODO — Milestone 3: Real-time Messaging

---

## 1. Kafka Event POJOs (`kafka/events/`)

- [x] `ChatMessageEvent`:
  - `messageId` (`Long`), `chatType` (`ChatType`), `roomId` (`Long`, nullable), `senderId` (`Long`), `recipientId` (`Long`, nullable), `content` (`String`), `replyToId` (`Long`, nullable), `attachmentIds` (`List<Long>`), `eventType` (`String` — `"CREATED"`, `"EDITED"`, `"DELETED"`), `createdAt` (`OffsetDateTime`)
- [x] `PresenceEvent`:
  - `userId` (`Long`), `username` (`String`), `status` (`String` — `"ONLINE"` or `"OFFLINE"`), `timestamp` (`OffsetDateTime`)

---

## 2. DTOs

### Request DTOs (`dto/request/`)
- [x] `SendMessageRequest`:
  - `content` (`@Size(max=3072)`)
  - `replyToId` (`Long`, optional)
  - `attachmentIds` (`List<Long>`, optional)
- [x] `EditMessageRequest`:
  - `content` (`@NotBlank @Size(max=3072)`)

### Response DTOs (`dto/response/`)
- [x] `AttachmentResponse`:
  - `id`, `originalFilename`, `fileSize` (`Long`), `mimeType`, `comment`, `downloadUrl` (`String`)
- [x] `MessageResponse`:
  - `id`, `chatType` (`ChatType`), `sender` (`UserResponse`), `content`, `replyTo` (`MessageResponse`, nullable), `attachments` (`List<AttachmentResponse>`), `editedAt`, `createdAt`

### EntityMapper updates (`util/EntityMapper.java`)
- [x] Add `toAttachmentResponse(Attachment attachment)` — `downloadUrl` = `"/api/attachments/" + attachment.getId()`
- [x] Add `toMessageResponse(Message message, List<AttachmentResponse> attachments)` — maps all fields; `replyTo` is `null` if no reply

---

## 3. AppConstants additions (`util/AppConstants.java`)

- [x] Add Kafka topic constants:
  - `TOPIC_CHAT_MESSAGES = "chat.messages"`
  - `TOPIC_PRESENCE_EVENTS = "presence.events"`
- [x] Add WebSocket destination constants:
  - `WS_TOPIC_ROOMS = "/topic/rooms/"`
  - `WS_QUEUE_MESSAGES = "/queue/messages"`

---

## 4. Repository additions

- [x] `MessageRepository` — add cursor-based pagination queries:
  - `@Query` method `findRoomMessages(Long roomId, OffsetDateTime before, Pageable pageable)` — returns messages where `room.id = roomId`, `deletedAt IS NULL`, `createdAt < before` (or all if `before` is null), ordered by `createdAt DESC`
  - `@Query` method `findPersonalMessages(Long userId1, Long userId2, OffsetDateTime before, Pageable pageable)` — returns `PERSONAL` messages between the two users (both directions), `deletedAt IS NULL`, `createdAt < before`, ordered `DESC`
- [x] `AttachmentRepository` — add:
  - `List<Attachment> findByMessageIdIn(List<Long> messageIds)` — batch-load attachments for a list of messages
  - `List<Attachment> findByMessageId(Long messageId)` — load attachments for a single message
- [x] `ContactRepository` — add:
  - `@Query` method `findAcceptedBetween(Long userId1, Long userId2)` → `Optional<Contact>` — finds an `ACCEPTED` contact record regardless of requester/addressee direction

---

## 5. WebSocket Configuration (`configuration/WebSocketConfig.java`)

- [x] `@Configuration @EnableWebSocketMessageBroker`
- [x] `registerStompEndpoints`: endpoint `/ws` with SockJS fallback, allowed origins `*`
- [x] `configureMessageBroker`: enable simple broker for `/topic` and `/user`; application destination prefix `/app`
- [x] `configureClientInboundChannel`: add `JwtChannelInterceptor` to authenticate STOMP `CONNECT` frames

### JWT Channel Interceptor (`websocket/JwtChannelInterceptor.java`)
- [x] Implements `ChannelInterceptor`
- [x] On `CONNECT`: extract `Authorization` header from STOMP native headers → validate JWT with `JwtTokenProvider` → set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` for the session
- [x] On invalid/missing token: throw `MessagingException`

---

## 6. WebSocket Event Listener (`websocket/StompEventListener.java`)

- [x] `@Component` implementing `ApplicationListener<SessionConnectEvent>` and `ApplicationListener<SessionDisconnectEvent>`
- [x] On connect: extract `userId` from principal → build `PresenceEvent(status="ONLINE")` → publish to `presence.events` via `PresenceEventProducer`
- [x] On disconnect: same, with `status="OFFLINE"`

---

## 7. Kafka Layer

### Producers
- [x] `kafka/producer/MessageEventProducer.java`:
  - `@Component`, inject `KafkaTemplate<String, ChatMessageEvent>`
  - `void send(ChatMessageEvent event)` — key = `roomId.toString()` for room messages, `"dm:{min}:{max}"` for personal; topic = `AppConstants.TOPIC_CHAT_MESSAGES`
- [x] `kafka/producer/PresenceEventProducer.java`:
  - `@Component`, inject `KafkaTemplate<String, PresenceEvent>`
  - `void send(PresenceEvent event)` — key = `userId.toString()`; topic = `AppConstants.TOPIC_PRESENCE_EVENTS`

### Consumer
- [x] `kafka/consumer/MessageEventConsumer.java`:
  - `@Component`, `@KafkaListener(topics = AppConstants.TOPIC_CHAT_MESSAGES, groupId = "chitchat")`
  - On `ChatMessageEvent`:
    - If `ROOM`: send to `/topic/rooms/{roomId}` via `SimpMessagingTemplate`
    - If `PERSONAL`: send to `/user/{recipientId}/queue/messages` via `SimpMessagingTemplate`

### Kafka Configuration (`configuration/KafkaConfig.java`)
- [x] Create `KafkaConfig.java`:
  - `ProducerFactory<String, Object>` bean — JSON serializer for values
  - `KafkaTemplate<String, Object>` bean
  - `ConsumerFactory<String, Object>` bean — JSON deserializer, trusted packages `com.chitchat.app.kafka.events`
  - `ConcurrentKafkaListenerContainerFactory` bean

---

## 8. Service Layer

### MessageService interface (`service/MessageService.java`)
- [x] Define all methods:
  - `MessageResponse sendRoomMessage(Long roomId, Long senderId, SendMessageRequest request)`
  - `MessageResponse sendPersonalMessage(Long recipientId, Long senderId, SendMessageRequest request)`
  - `MessageResponse editMessage(Long messageId, Long requesterId, EditMessageRequest request)`
  - `void deleteMessage(Long messageId, Long requesterId)`
  - `List<MessageResponse> getRoomMessages(Long roomId, Long requesterId, OffsetDateTime before, int limit)`
  - `List<MessageResponse> getPersonalMessages(Long userId, Long requesterId, OffsetDateTime before, int limit)`

### MessageServiceImpl (`service/MessageServiceImpl.java`)
- [x] `sendRoomMessage`:
  - Load room → `ResourceNotFoundException` if absent
  - Verify sender is a room member → `ForbiddenException`
  - If `replyToId` present, load reply message → `ResourceNotFoundException` if absent
  - Save `Message` (chatType=ROOM, room, sender, content, replyTo, createdAt)
  - Publish `ChatMessageEvent(eventType="CREATED")` via `MessageEventProducer`
  - Return `MessageResponse`
- [x] `sendPersonalMessage`:
  - Load recipient → `ResourceNotFoundException` if absent
  - Check accepted friendship via `contactRepository.findAcceptedBetween` → `ForbiddenException` if not friends
  - Check neither banned the other via `userBanRepository` (two calls) → `ForbiddenException` if banned
  - Save `Message` (chatType=PERSONAL, sender, recipient, content, replyTo, createdAt)
  - Publish `ChatMessageEvent(eventType="CREATED")` via `MessageEventProducer`
  - Return `MessageResponse`
- [x] `editMessage`:
  - Load message → `ResourceNotFoundException` if absent or soft-deleted
  - Verify requester is the author → `ForbiddenException`
  - Update `content`, set `editedAt = now()`, save
  - Publish `ChatMessageEvent(eventType="EDITED")` via `MessageEventProducer`
  - Return `MessageResponse`
- [x] `deleteMessage`:
  - Load message → `ResourceNotFoundException` if absent or soft-deleted
  - For ROOM messages: allow author OR room admin/owner → `ForbiddenException` otherwise
  - For PERSONAL messages: allow author only → `ForbiddenException` otherwise
  - Soft-delete: set `deletedAt = now()`, save
  - Publish `ChatMessageEvent(eventType="DELETED")` via `MessageEventProducer`
- [x] `getRoomMessages`:
  - Load room → `ResourceNotFoundException` if absent
  - If PRIVATE: verify requester is a member → `ForbiddenException`
  - Query `messageRepository.findRoomMessages(roomId, before, PageRequest.of(0, limit))`
  - Batch-load attachments via `attachmentRepository.findByMessageIdIn`
  - Return `List<MessageResponse>`
- [x] `getPersonalMessages`:
  - Verify requester is one of the two participants → `ForbiddenException`
  - Query `messageRepository.findPersonalMessages(requesterId, userId, before, PageRequest.of(0, limit))`
  - Batch-load attachments
  - Return `List<MessageResponse>`

### AttachmentService interface (`service/AttachmentService.java`)
- [x] Define methods:
  - `AttachmentResponse upload(Long uploaderId, MultipartFile file, String comment, String contextType, Long contextId)`
  - `org.springframework.core.io.Resource download(Long attachmentId, Long requesterId)`

### AttachmentServiceImpl (`service/AttachmentServiceImpl.java`)
- [x] `upload`:
  - Validate file size: images (`image/*`) ≤ 3 MB; others ≤ 20 MB → `ValidationException` if exceeded
  - Generate unique stored filename (`UUID + original extension`); write to `${app.storage.location}/`
  - Save `Attachment` entity (uploaderId, originalFilename, storedPath, fileSize, mimeType, comment, createdAt)
  - Return `AttachmentResponse`
- [x] `download`:
  - Load attachment → `ResourceNotFoundException` if absent
  - Access check: if linked to a room message → requester must be room member; if personal → sender or recipient; if unlinked → uploader only → `ForbiddenException`
  - Return `UrlResource` pointing to stored file path

---

## 9. WebSocket Message Controller (`websocket/RoomMessageWsController.java`)

- [x] `@Controller`
- [x] `@MessageMapping("/rooms/{roomId}/send")` → calls `messageService.sendRoomMessage`; result published to Kafka; consumer fans out to `/topic/rooms/{roomId}`
- [x] `@MessageMapping("/chats/{userId}/send")` → calls `messageService.sendPersonalMessage`; consumer fans out to `/user/{recipientId}/queue/messages`

---

## 10. REST Controllers

### MessageController (`rest/MessageController.java`)
- [x] `GET /api/rooms/{roomId}/messages` — params: `before` (ISO datetime, optional), `limit` (default 50, max 100); returns 200 + `List<MessageResponse>`
- [x] `PUT /api/messages/{messageId}` — `@Valid`; returns 200 + `MessageResponse`
- [x] `DELETE /api/messages/{messageId}` — returns 204
- [x] `GET /api/chats/{userId}/messages` — params: `before`, `limit`; returns 200 + `List<MessageResponse>`

### AttachmentController (`rest/AttachmentController.java`)
- [x] `POST /api/attachments` — `multipart/form-data`: `file`, `comment` (optional), `contextType` (optional), `contextId` (optional); returns 201 + `AttachmentResponse`
- [x] `GET /api/attachments/{attachmentId}` — streams file with `Content-Disposition` and `Content-Type` headers; returns 200

---

## 11. Integration Tests (`src/test/java/com/chitchat/app/service/MessageServiceImplTest.java`)

- [x] `sendRoomMessage_success` — message saved, Kafka event published
- [x] `sendRoomMessage_notMember` → `ForbiddenException`
- [x] `sendRoomMessage_withReply_success` — replyTo field set
- [x] `sendPersonalMessage_success` — friendship confirmed, message saved
- [x] `sendPersonalMessage_notFriends` → `ForbiddenException`
- [x] `sendPersonalMessage_senderBannedByRecipient` → `ForbiddenException`
- [x] `sendPersonalMessage_recipientBannedBySender` → `ForbiddenException`
- [x] `editMessage_success_authorOnly` — content updated, `editedAt` set
- [x] `editMessage_notAuthor` → `ForbiddenException`
- [x] `editMessage_deletedMessage` → `ResourceNotFoundException`
- [x] `deleteMessage_byAuthor_success` — `deletedAt` set
- [x] `deleteMessage_byRoomAdmin_success` — admin can soft-delete
- [x] `deleteMessage_unauthorized` → `ForbiddenException`
- [x] `getRoomMessages_publicRoom_success` — returns paginated list
- [x] `getRoomMessages_privateRoom_nonMember` → `ForbiddenException`
- [x] `getPersonalMessages_success`
- [x] `getPersonalMessages_notParticipant` → `ForbiddenException`

---

## 12. Smoke Test Checklist (Manual Verification)

- [x] `POST /api/attachments` — 201; file written to `${app.storage.location}`
- [x] `GET /api/attachments/{id}` — binary file returned; 403 for non-member
- [x] Connect WebSocket client to `/ws`; subscribe to `/topic/rooms/{roomId}`
- [x] Send message via REST `POST /api/rooms/{roomId}/messages` → 201; WebSocket subscriber receives `MessageEvent`
- [x] Edit via `PUT /api/messages/{id}` → WebSocket delivers `EDITED` event
- [x] Delete via `DELETE /api/messages/{id}` → WebSocket delivers `DELETED` event
- [x] `GET /api/rooms/{roomId}/messages?limit=10` — returns up to 10 messages newest-first
- [x] `GET /api/rooms/{roomId}/messages?before=<timestamp>&limit=10` — cursor pagination works
- [x] Send personal message between two friends → received on `/user/queue/messages`
- [x] Attempt personal message without friendship → 403
- [x] `mvn test` — all tests pass
