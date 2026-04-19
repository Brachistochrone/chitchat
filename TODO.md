# TODO — Milestone 4: Presence & Notifications

---

## 1. New Enums & Event POJOs

### New enum `entity/enums/PresenceAction.java`
- [x] `CONNECT`, `DISCONNECT`, `HEARTBEAT`

### Update `kafka/events/PresenceEvent.java`
- [x] Add `action` field (`PresenceAction`) — distinguishes connect/disconnect/heartbeat for the Kafka Streams aggregate

### New POJO `kafka/events/PresenceStateEvent.java`
- [x] `userId` (`Long`), `username` (`String`), `status` (`PresenceStatus`), `tabCount` (`int`), `lastHeartbeatMs` (`long`)
- [x] Output of the Kafka Streams KTable — published to `presence.state` topic

### New enum `entity/enums/NotificationType.java`
- [x] `FRIEND_REQUEST`, `ROOM_INVITE`, `UNREAD_UPDATE`

### New POJO `kafka/events/NotificationEvent.java`
- [x] `type` (`NotificationType`), `targetUserId` (`Long`), `payload` (`String` — JSON-encoded data), `timestamp` (`OffsetDateTime`)

### Update `entity/enums/KafkaTopic.java`
- [x] Add `PRESENCE_STATE("presence.state")`
- [x] Add `NOTIFICATIONS("notifications")`

---

## 2. Database — Unread Counts

### Flyway migration `V11__create_unread_counts.sql`
- [x] Table `unread_counts`:
  - `id` (`BIGSERIAL PK`)
  - `user_id` (`BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE`)
  - `room_id` (`BIGINT REFERENCES rooms(id) ON DELETE CASCADE`, nullable)
  - `chat_user_id` (`BIGINT REFERENCES users(id) ON DELETE CASCADE`, nullable)
  - `count` (`INT NOT NULL DEFAULT 0`)
  - `CHECK` constraint: exactly one of `room_id` or `chat_user_id` is non-null
  - `UNIQUE (user_id, room_id)`
  - `UNIQUE (user_id, chat_user_id)`

### Entity `entity/UnreadCount.java`
- [x] `id`, `user` (`@ManyToOne` → User), `room` (`@ManyToOne` → Room, nullable), `chatUser` (`@ManyToOne` → User, nullable), `count` (`int`)

### Repository `dao/UnreadCountRepository.java`
- [x] `Optional<UnreadCount> findByUserIdAndRoomId(Long userId, Long roomId)`
- [x] `Optional<UnreadCount> findByUserIdAndChatUserId(Long userId, Long chatUserId)`
- [x] `List<UnreadCount> findByUserIdAndCountGreaterThan(Long userId, int count)` — for listing all chats with unreads

---

## 3. Kafka Streams — Presence Aggregation

### Kafka Streams configuration `configuration/KafkaStreamsConfig.java`
- [x] `@Configuration @EnableKafkaStreams`
- [x] `KafkaStreamsConfiguration` bean:
  - `APPLICATION_ID_CONFIG = "chitchat-presence"`
  - `BOOTSTRAP_SERVERS_CONFIG` from `${spring.kafka.bootstrap-servers}`
  - `DEFAULT_KEY_SERDE_CLASS_CONFIG = Serdes.StringSerde.class`
  - `DEFAULT_VALUE_SERDE_CLASS_CONFIG = Serdes.StringSerde.class`

### Presence topology `kafka/streams/PresenceStateTopology.java`
- [x] `@Component` with `@Bean KStream<String, String>` building the topology via `StreamsBuilder`
- [x] Read from `presence.events` topic (String key = userId, String value = PresenceEvent JSON)
- [x] `groupByKey()` → `aggregate()`:
  - Initial value: `PresenceState(tabCount=0, lastHeartbeatMs=0, status=OFFLINE)`
  - On `CONNECT`: `tabCount++`, `lastHeartbeatMs = now`, derive status
  - On `DISCONNECT`: `tabCount--`, derive status
  - On `HEARTBEAT`: `lastHeartbeatMs = now`, derive status
  - Derive status: `tabCount == 0 → OFFLINE`, `now - lastHeartbeatMs < 60_000 → ONLINE`, else `AFK`
- [x] `toStream()` → map to `PresenceStateEvent` JSON → `to("presence.state")`
- [x] Use `ObjectMapper` for JSON serialization/deserialization within the topology

---

## 4. Heartbeat Handler (`websocket/PresenceHeartbeatHandler.java`)

- [x] `@Controller @RequiredArgsConstructor`
- [x] `@MessageMapping("/presence/heartbeat")`:
  - Extract `userId` from `Principal`
  - Publish `PresenceEvent(action=HEARTBEAT, status=ONLINE)` to `presence.events` via `PresenceEventProducer`
  - Track `lastHeartbeatMs` per userId in `ConcurrentHashMap`
- [x] `@Scheduled(fixedRate = 30_000)` — AFK timer:
  - Iterate all tracked users
  - If `now - lastHeartbeat > 60_000` and user not already marked AFK: publish `PresenceEvent(action=HEARTBEAT, status=AFK)`
  - Mark as AFK in local map to avoid re-publishing
  - Remove entries for users who have disconnected (tabCount == 0)

### Update `StompEventListener.java`
- [x] Set `action = PresenceAction.CONNECT` for connect events
- [x] Set `action = PresenceAction.DISCONNECT` for disconnect events
- [x] On disconnect: remove userId from heartbeat tracking map in `PresenceHeartbeatHandler`

### AppConstants additions
- [x] Add `WS_TOPIC_ROOMS_PRESENCE_SUFFIX = "/presence"` (for `/topic/rooms/{roomId}/presence`)

---

## 5. Presence State Consumer (`kafka/consumer/PresenceStateConsumer.java`)

- [x] `@Component @RequiredArgsConstructor`
- [x] `@KafkaListener(topics = "presence.state", groupId = "chitchat")`
- [x] On `PresenceStateEvent`:
  - Parse JSON to `PresenceStateEvent`
  - Look up user's rooms via `RoomMemberRepository.findByIdUserId(userId)`
  - For each room: send to `/topic/rooms/{roomId}/presence` via `SimpMessagingTemplate`
  - Payload: `{ "userId": ..., "username": ..., "status": "ONLINE|AFK|OFFLINE" }`

---

## 6. Notification Layer

### Producer `kafka/producer/NotificationEventProducer.java`
- [x] `@Component`, inject `KafkaTemplate<String, Object>`
- [x] `void send(NotificationEvent event)` — key = `targetUserId.toString()`; topic = `KafkaTopic.NOTIFICATIONS.getTopicName()`

### Consumer `kafka/consumer/NotificationConsumer.java`
- [x] `@Component @RequiredArgsConstructor`
- [x] `@KafkaListener(topics = "notifications", groupId = "chitchat")`
- [x] On `NotificationEvent`:
  - Parse JSON
  - Send to `/user/{targetUserId}/queue/notifications` via `SimpMessagingTemplate`

---

## 7. Notification Service

### Interface `service/NotificationService.java`
- [x] `void incrementRoomUnread(Long roomId, Long senderIdToExclude)`
- [x] `void incrementPersonalUnread(Long recipientId, Long senderId)`
- [x] `void resetRoomUnread(Long userId, Long roomId)`
- [x] `void resetPersonalUnread(Long userId, Long chatUserId)`
- [x] `List<UnreadCountResponse> getUnreadCounts(Long userId)`

### Impl `service/NotificationServiceImpl.java`
- [x] `incrementRoomUnread`:
  - Load all room members via `RoomMemberRepository.findByIdRoomId(roomId)`
  - For each member except `senderIdToExclude`: upsert `UnreadCount` record, `count++`
  - Publish `NotificationEvent(type=UNREAD_UPDATE)` for each affected user via `NotificationEventProducer`
- [x] `incrementPersonalUnread`:
  - Upsert `UnreadCount` for `(recipientId, chatUserId=senderId)`, `count++`
  - Publish `NotificationEvent(type=UNREAD_UPDATE)` for recipient
- [x] `resetRoomUnread`:
  - Find `UnreadCount` by `(userId, roomId)`, set `count = 0`
- [x] `resetPersonalUnread`:
  - Find `UnreadCount` by `(userId, chatUserId)`, set `count = 0`
- [x] `getUnreadCounts`:
  - Query `UnreadCountRepository.findByUserIdAndCountGreaterThan(userId, 0)`
  - Return list of `UnreadCountResponse`

### Response DTO `dto/response/UnreadCountResponse.java`
- [x] `roomId` (`Long`, nullable), `chatUserId` (`Long`, nullable), `count` (`int`)

### EntityMapper update
- [x] Add `toUnreadCountResponse(UnreadCount unreadCount)` static method

---

## 8. Integration with MessageService

### Update `MessageServiceImpl.java`
- [x] Inject `NotificationService`
- [x] After `sendRoomMessage` saves message: call `notificationService.incrementRoomUnread(roomId, senderId)`
- [x] After `sendPersonalMessage` saves message: call `notificationService.incrementPersonalUnread(recipientId, senderId)`

---

## 9. REST Endpoints for Unread Counts

### Update `MessageController.java` or new `NotificationController.java`
- [x] `GET /api/notifications/unread` → `getUnreadCounts`; returns 200 + `List<UnreadCountResponse>`
- [x] `POST /api/rooms/{roomId}/messages/read` → `resetRoomUnread`; returns 204
- [x] `POST /api/chats/{userId}/messages/read` → `resetPersonalUnread`; returns 204

---

## 10. Enable Scheduling

### Update or create `configuration/SchedulingConfig.java`
- [x] `@Configuration @EnableScheduling`

---

## 11. Integration Tests

### `src/test/java/com/chitchat/app/service/NotificationServiceImplTest.java`
- [x] `incrementRoomUnread_success` — unread count incremented for all members except sender
- [x] `incrementRoomUnread_excludesSender` — sender's count not incremented
- [x] `incrementPersonalUnread_success` — recipient's count incremented
- [x] `resetRoomUnread_success` — count set to 0
- [x] `resetPersonalUnread_success` — count set to 0
- [x] `getUnreadCounts_returnsOnlyNonZero`

### `src/test/java/com/chitchat/app/kafka/streams/PresenceStateTopologyTest.java`
- [x] `connect_incrementsTabCount_statusOnline`
- [x] `disconnect_decrementsTabCount_statusOffline`
- [x] `multipleConnects_thenOneDisconnect_remainsOnline`
- [x] `heartbeat_updatesLastHeartbeat`
- [x] `noHeartbeat_afkEventReceived_statusAfk`

---

## 12. Access-Control & Routing Summary

| Component | Input | Output |
|---|---|---|
| `StompEventListener` | WebSocket CONNECT/DISCONNECT | `presence.events` Kafka topic |
| `PresenceHeartbeatHandler` | `/app/presence/heartbeat` (WebSocket) + `@Scheduled` | `presence.events` Kafka topic |
| `PresenceStateTopology` (Kafka Streams) | `presence.events` topic | `presence.state` topic (KTable changelog) |
| `PresenceStateConsumer` | `presence.state` topic | `/topic/rooms/{roomId}/presence` (WebSocket) |
| `NotificationEventProducer` | Service calls | `notifications` topic |
| `NotificationConsumer` | `notifications` topic | `/user/{userId}/queue/notifications` (WebSocket) |
| `NotificationService` | Message send events | DB `unread_counts` + notifications topic |

---

## 13. Smoke Test Checklist (Manual Verification)

- [x] Connect WebSocket, subscribe to `/topic/rooms/{roomId}/presence` → receive `ONLINE` event
- [x] Disconnect WebSocket → subscribers receive `OFFLINE` event
- [x] Send heartbeat every 30s → status stays `ONLINE`
- [x] Stop heartbeats for 60s+ → subscribers receive `AFK` event
- [x] Open two tabs (two WebSocket connections) → close one → status stays `ONLINE`
- [x] Close both tabs → status becomes `OFFLINE`
- [x] Send a room message → all members (except sender) get `UNREAD_UPDATE` notification
- [x] `GET /api/notifications/unread` → returns non-zero counts
- [x] `POST /api/rooms/{roomId}/messages/read` → resets room unread count to 0
- [x] Send personal message → recipient gets `UNREAD_UPDATE` notification
- [x] `POST /api/chats/{userId}/messages/read` → resets personal unread count to 0
- [x] `mvn test` — all tests pass
