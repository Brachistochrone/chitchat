# Chitchat — Full Technical Specification

---

## 1. Full Requirements

### 1.1 User Accounts & Authentication
- Self-registration with email, password, and unique username.
- Email and username must each be unique system-wide; username is immutable after registration.
- Sign in with email + password; persistent login across browser sessions (no auto-logout).
- Password reset via email token; authenticated password change.
- Secure password hashing (bcrypt).
- Account deletion: permanently removes owned rooms (including all their messages and files), all of the user's messages and uploaded files, and their membership records in other rooms.

### 1.2 User Presence & Sessions
- Three statuses: **online**, **AFK**, **offline**.
- AFK triggered after ≥ 1 minute of inactivity across all open tabs.
- Multi-tab: user is online if at least one tab is active; offline only when all tabs are closed.
- Users can view and revoke their own active sessions (browser agent + IP shown).
- Logout terminates only the current session; other sessions are unaffected.
- Presence updates delivered to clients in < 2 seconds.

### 1.3 Contacts / Friends
- Each user has a personal contact list.
- Friend requests sent by username or from a room's member list, with an optional message.
- Friendship is confirmed by the recipient; neither side is friends until confirmed.
- Either party may remove a friend at any time.
- Either party may ban another user: blocks messaging, terminates the friendship, leaves message history read-only for both.
- Personal (1-to-1) messaging is only allowed if both users are friends and neither has banned the other.

### 1.4 Chat Rooms
- Any authenticated user may create a room.
- Room properties: name (globally unique), description, visibility (`PUBLIC` | `PRIVATE`), owner, admins list, members list, banned-users list.
- **Public** rooms appear in the searchable catalog; any non-banned user may join freely.
- **Private** rooms are invite-only and do not appear in the catalog.
- Users may freely join/leave public rooms (unless banned); the owner cannot leave their own room.
- Deleting a room permanently removes all its messages and attachments.
- **Owner**: always an admin; cannot be demoted; the only one who can delete the room.
- **Admin**: can delete any message in the room, kick (remove) members, ban/unban members, and demote other admins (but not the owner).
- Room ban: the user is removed immediately and cannot rejoin until explicitly unbanned.
- Banned users lose access to the room's messages and files.

### 1.5 Messaging
- Room chats and personal (1-to-1) chats share identical feature sets.
- A personal dialog is a two-participant chat.
- Message content: plain/multi-line text, emoji, zero or more attachments, optional reply-to reference.
- Maximum message body: 3 KB, UTF-8.
- Replies are visually quoted (reference carried in the message record).
- Edited messages show an "edited" indicator.
- Messages may be deleted by their author or by a room admin.
- All messages are stored and delivered to offline users on reconnect.
- Messages are ordered chronologically; clients support infinite scroll for history.

### 1.6 Attachments
- Supported: images (≤ 3 MB) and arbitrary files (≤ 20 MB).
- Upload via UI button or copy-paste.
- Original filename is preserved; an optional text comment may accompany the attachment.
- Stored on the local filesystem; metadata in the database.
- Access control: only current members / authorized chat participants may download.
- Files persist after upload; a user who is banned or removed loses access but the file is not deleted.

### 1.7 Notifications
- Unread-message indicators displayed per room and per contact.
- Presence updates (online/AFK/offline) propagated to relevant clients in < 2 seconds.

### 1.8 Non-Functional Requirements
| Attribute | Target |
|---|---|
| Concurrent users | 300 |
| Members per room | Up to 1,000 |
| Messages per room | 10,000+ (infinite scroll) |
| Message delivery latency | ≤ 3 seconds |
| Presence update latency | < 2 seconds |
| File storage | Local filesystem |
| Max file size | 20 MB (images: 3 MB) |
| Session persistence | Survives browser close/open; no auto-logout |
| Deployment | Docker Compose (app + PostgreSQL + Kafka) |

---

## 2. Package Structure

```
com.chitchat.app
│
├── configuration/
│   ├── KafkaConfig.java              # Producer/consumer factory beans
│   ├── KafkaStreamsConfig.java        # Presence aggregation topology
│   ├── WebSocketConfig.java          # STOMP over SockJS endpoint registration
│   ├── SecurityConfig.java           # Spring Security filter chain, CORS, JWT
│   ├── CacheConfig.java              # Caffeine cache manager definitions
│   └── OpenApiConfig.java            # SpringDoc / Swagger configuration
│
├── rest/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── SessionController.java
│   ├── RoomController.java
│   ├── RoomMemberController.java
│   ├── MessageController.java
│   ├── ContactController.java
│   └── AttachmentController.java
│
├── graphql/
│   ├── query/
│   │   ├── UserQueryResolver.java
│   │   ├── RoomQueryResolver.java
│   │   └── MessageQueryResolver.java
│   ├── mutation/
│   │   ├── AuthMutationResolver.java
│   │   ├── RoomMutationResolver.java
│   │   ├── MessageMutationResolver.java
│   │   └── ContactMutationResolver.java
│   └── subscription/
│       ├── MessageSubscriptionResolver.java
│       ├── PresenceSubscriptionResolver.java
│       └── NotificationSubscriptionResolver.java
│
├── websocket/
│   ├── StompEventListener.java       # Connect/disconnect hooks → presence events
│   └── PresenceHeartbeatHandler.java # AFK timer per session
│
├── service/
│   ├── AuthService.java              + AuthServiceImpl.java
│   ├── UserService.java              + UserServiceImpl.java
│   ├── SessionService.java           + SessionServiceImpl.java
│   ├── RoomService.java              + RoomServiceImpl.java
│   ├── MessageService.java           + MessageServiceImpl.java
│   ├── ContactService.java           + ContactServiceImpl.java
│   ├── AttachmentService.java        + AttachmentServiceImpl.java
│   ├── PresenceService.java          + PresenceServiceImpl.java
│   └── NotificationService.java      + NotificationServiceImpl.java
│
├── dao/
│   ├── UserRepository.java
│   ├── UserSessionRepository.java
│   ├── RoomRepository.java
│   ├── RoomMemberRepository.java
│   ├── RoomBanRepository.java
│   ├── RoomInviteRepository.java
│   ├── MessageRepository.java
│   ├── AttachmentRepository.java
│   ├── ContactRepository.java
│   └── UserBanRepository.java
│
├── entity/
│   ├── User.java
│   ├── UserSession.java
│   ├── Room.java
│   ├── RoomMember.java               # Composite PK: (room_id, user_id)
│   ├── RoomBan.java
│   ├── RoomInvite.java
│   ├── Message.java
│   ├── Attachment.java
│   ├── Contact.java                  # Represents a friend-request/friendship
│   └── UserBan.java                  # User-level ban (between two users)
│
├── dto/
│   ├── request/                      # One DTO per write operation
│   └── response/                     # One DTO per read operation
│
├── kafka/
│   ├── producer/
│   │   ├── MessageEventProducer.java
│   │   ├── PresenceEventProducer.java
│   │   └── NotificationEventProducer.java
│   ├── consumer/
│   │   ├── MessageEventConsumer.java  # Fans out to WebSocket subscribers
│   │   └── NotificationConsumer.java
│   └── streams/
│       └── PresenceStateTopology.java # Kafka Streams: events → KTable
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── AccessDeniedException.java
│   ├── ConflictException.java
│   └── ValidationException.java
│
├── util/
│   ├── FileStorageUtil.java
│   └── PageableUtil.java
│
└── security/
    ├── JwtTokenProvider.java
    ├── JwtAuthenticationFilter.java
    └── UserDetailsServiceImpl.java
```

---

## 3. REST Endpoints

All endpoints are prefixed `/api`. Request/response bodies are JSON unless noted.  
Authentication: Bearer JWT in `Authorization` header (except `/auth/*`).  
File endpoints use `multipart/form-data` (upload) and `application/octet-stream` (download).

### 3.1 Auth

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| POST | `/auth/register` | `{ email, password, username }` | `UserResponse` | 201 |
| POST | `/auth/login` | `{ email, password }` | `{ token, user: UserResponse }` | |
| POST | `/auth/logout` | — | — | Invalidates current session |
| POST | `/auth/password-reset/request` | `{ email }` | — | Sends reset email |
| POST | `/auth/password-reset/confirm` | `{ token, newPassword }` | — | |

### 3.2 Users

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| GET | `/users/me` | — | `UserResponse` | |
| PUT | `/users/me` | `{ displayName?, avatarUrl? }` | `UserResponse` | |
| PUT | `/users/me/password` | `{ currentPassword, newPassword }` | — | |
| DELETE | `/users/me` | — | — | Full account deletion |
| GET | `/users/{username}` | — | `UserResponse` | Lookup by username |

### 3.3 Sessions

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| GET | `/sessions` | — | `List<SessionResponse>` | Current user's sessions |
| DELETE | `/sessions/{sessionId}` | — | — | Revoke a specific session |

### 3.4 Contacts

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| GET | `/contacts` | — | `List<ContactResponse>` | Accepted friends |
| POST | `/contacts/requests` | `{ targetUsername, message? }` | `ContactResponse` | Send friend request |
| GET | `/contacts/requests/incoming` | — | `List<ContactResponse>` | Pending requests |
| PUT | `/contacts/requests/{requestId}/accept` | — | `ContactResponse` | |
| DELETE | `/contacts/requests/{requestId}` | — | — | Decline or cancel |
| DELETE | `/contacts/{userId}` | — | — | Remove friend |
| POST | `/contacts/{userId}/ban` | — | — | Ban user |
| DELETE | `/contacts/{userId}/ban` | — | — | Unban user |

### 3.5 Rooms

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| GET | `/rooms` | query: `?q=&page=&size=` | `Page<RoomResponse>` | Public catalog, searchable |
| POST | `/rooms` | `{ name, description?, visibility }` | `RoomResponse` | 201 |
| GET | `/rooms/{roomId}` | — | `RoomResponse` | |
| PUT | `/rooms/{roomId}` | `{ name?, description?, visibility? }` | `RoomResponse` | Owner only |
| DELETE | `/rooms/{roomId}` | — | — | Owner only |
| POST | `/rooms/{roomId}/join` | — | — | Public rooms only |
| POST | `/rooms/{roomId}/leave` | — | — | Owner cannot leave |

### 3.6 Room Members

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| GET | `/rooms/{roomId}/members` | — | `List<MemberResponse>` | |
| POST | `/rooms/{roomId}/invites` | `{ username }` | — | Private rooms; owner/admin |
| POST | `/rooms/{roomId}/admins/{userId}` | — | — | Promote to admin; owner only |
| DELETE | `/rooms/{roomId}/admins/{userId}` | — | — | Demote admin; owner or self |
| POST | `/rooms/{roomId}/members/{userId}/kick` | — | — | Admin only |
| GET | `/rooms/{roomId}/bans` | — | `List<BanResponse>` | Admin only |
| POST | `/rooms/{roomId}/bans/{userId}` | — | — | Admin only |
| DELETE | `/rooms/{roomId}/bans/{userId}` | — | — | Admin only |

### 3.7 Messages

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| GET | `/rooms/{roomId}/messages` | query: `?before=&limit=` | `List<MessageResponse>` | Cursor-based, newest-first |
| PUT | `/messages/{messageId}` | `{ content }` | `MessageResponse` | Author only; sets edited_at |
| DELETE | `/messages/{messageId}` | — | — | Author or room admin |
| GET | `/chats/{userId}/messages` | query: `?before=&limit=` | `List<MessageResponse>` | Personal chat history |

### 3.8 Attachments

| Method | Path | Request body | Response body | Notes |
|---|---|---|---|---|
| POST | `/attachments` | `multipart: file, comment?, contextType (ROOM\|PERSONAL), contextId` | `AttachmentResponse` | Max 20 MB / 3 MB image |
| GET | `/attachments/{attachmentId}` | — | Binary stream | Access-controlled |

---

## 4. WebSocket (STOMP over SockJS)

**Endpoint:** `ws://.../ws` (SockJS fallback: `/ws`)

### Client → Server (destinations prefixed `/app`)

| Destination | Payload | Description |
|---|---|---|
| `/app/rooms/{roomId}/send` | `{ content, replyToId?, attachmentIds[] }` | Send message to room |
| `/app/chats/{userId}/send` | `{ content, replyToId?, attachmentIds[] }` | Send personal message |
| `/app/presence/heartbeat` | `{ status: ONLINE\|AFK }` | Tab activity ping (every 30 s) |

### Server → Client (subscriptions)

| Topic | Payload | Description |
|---|---|---|
| `/topic/rooms/{roomId}` | `MessageEvent` | New/edited/deleted messages in room |
| `/topic/rooms/{roomId}/presence` | `PresenceEvent` | Member presence changes in room |
| `/user/queue/messages` | `MessageEvent` | Incoming personal messages |
| `/user/queue/notifications` | `NotificationEvent` | Friend requests, invites, unread counts |

### Event Shapes

```json
// MessageEvent
{ "type": "CREATED|EDITED|DELETED", "message": MessageResponse }

// PresenceEvent
{ "userId": 1, "username": "alice", "status": "ONLINE|AFK|OFFLINE" }

// NotificationEvent
{ "type": "FRIEND_REQUEST|ROOM_INVITE|UNREAD_UPDATE", "payload": { ... } }
```

---

## 5. Kafka Topics

| Topic | Key | Value schema | Partitions | Description |
|---|---|---|---|---|
| `chat.messages` | `roomId` or `"dm:{userId1}:{userId2}"` | `ChatMessageEvent` JSON | 6 | All sent messages (rooms + personal) |
| `presence.events` | `userId` | `PresenceEvent` JSON | 3 | Connect/disconnect/AFK/heartbeat events |
| `presence.state` | `userId` | `PresenceStateEvent` JSON | 3 | Kafka Streams KTable changelog (aggregated state) |
| `room.events` | `roomId` | `RoomEvent` JSON | 3 | Join/leave/kick/ban events |
| `notifications` | `targetUserId` | `NotificationEvent` JSON | 3 | Friend requests, invites, unread badges |

### Kafka Streams Topology (Presence)

```
presence.events  →  groupByKey  →  aggregate (tab counter per userId)
                                 →  KTable<userId, PresenceStateEvent>
                                 →  toStream  →  presence.state topic
```

Logic: increment tab count on `CONNECT`, decrement on `DISCONNECT`; derive status as `ONLINE` (count > 0 and last heartbeat < 1 min ago), `AFK` (count > 0 and last heartbeat ≥ 1 min), `OFFLINE` (count == 0).

---

## 6. Database Schema

All tables use `BIGSERIAL` primary keys unless noted. Timestamps are `TIMESTAMPTZ`.

```sql
-- users
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    display_name  VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

-- user_sessions (persistent login tokens)
CREATE TABLE user_sessions (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    browser      VARCHAR(255),
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked      BOOLEAN     NOT NULL DEFAULT FALSE
);

-- rooms
CREATE TABLE rooms (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    visibility  VARCHAR(10)  NOT NULL CHECK (visibility IN ('PUBLIC','PRIVATE')),
    owner_id    BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- room_members
CREATE TABLE room_members (
    room_id   BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role      VARCHAR(10) NOT NULL CHECK (role IN ('OWNER','ADMIN','MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);

-- room_bans
CREATE TABLE room_bans (
    room_id    BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by  BIGINT      NOT NULL REFERENCES users(id),
    banned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);

-- room_invites (private rooms)
CREATE TABLE room_invites (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    invited_user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_by      BIGINT      NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, invited_user_id)
);

-- contacts (friend requests + friendships)
CREATE TABLE contacts (
    id          BIGSERIAL PRIMARY KEY,
    requester_id BIGINT     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id BIGINT     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(10) NOT NULL CHECK (status IN ('PENDING','ACCEPTED')),
    message     VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (requester_id, addressee_id)
);

-- user_bans (user-to-user block)
CREATE TABLE user_bans (
    id            BIGSERIAL PRIMARY KEY,
    banner_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (banner_id, banned_id)
);

-- messages
CREATE TABLE messages (
    id          BIGSERIAL PRIMARY KEY,
    chat_type   VARCHAR(10)  NOT NULL CHECK (chat_type IN ('ROOM','PERSONAL')),
    room_id     BIGINT       REFERENCES rooms(id) ON DELETE SET NULL,
    sender_id   BIGINT       NOT NULL REFERENCES users(id),
    recipient_id BIGINT      REFERENCES users(id),           -- personal chats only
    content     VARCHAR(3072),
    reply_to_id BIGINT       REFERENCES messages(id),
    edited_at   TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (
        (chat_type = 'ROOM'     AND room_id IS NOT NULL AND recipient_id IS NULL) OR
        (chat_type = 'PERSONAL' AND recipient_id IS NOT NULL AND room_id IS NULL)
    )
);
CREATE INDEX idx_messages_room     ON messages (room_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_messages_personal ON messages (sender_id, recipient_id, created_at DESC) WHERE deleted_at IS NULL;

-- attachments
CREATE TABLE attachments (
    id                BIGSERIAL PRIMARY KEY,
    message_id        BIGINT       REFERENCES messages(id) ON DELETE SET NULL,
    uploader_id       BIGINT       NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_path       VARCHAR(512) NOT NULL,
    file_size         BIGINT       NOT NULL,
    mime_type         VARCHAR(127),
    comment           VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

---

## 7. GraphQL Schema

Binary file operations (upload/download) remain on REST. Everything else is served via GraphQL.

```graphql
# ── Scalars ───────────────────────────────────────────────────────────
scalar DateTime
scalar Upload

# ── Enums ─────────────────────────────────────────────────────────────
enum RoomVisibility { PUBLIC PRIVATE }
enum RoomRole       { OWNER ADMIN MEMBER }
enum PresenceStatus { ONLINE AFK OFFLINE }
enum ContactStatus  { PENDING ACCEPTED }
enum ChatType       { ROOM PERSONAL }
enum EventType      { CREATED EDITED DELETED }

# ── Types ─────────────────────────────────────────────────────────────
type User {
    id:          ID!
    username:    String!
    displayName: String
    presence:    PresenceStatus!
    createdAt:   DateTime!
}

type Room {
    id:          ID!
    name:        String!
    description: String
    visibility:  RoomVisibility!
    owner:       User!
    memberCount: Int!
    createdAt:   DateTime!
}

type RoomMember {
    user:     User!
    role:     RoomRole!
    joinedAt: DateTime!
}

type Message {
    id:          ID!
    chatType:    ChatType!
    sender:      User!
    content:     String
    replyTo:     Message
    attachments: [Attachment!]!
    editedAt:    DateTime
    createdAt:   DateTime!
}

type Attachment {
    id:               ID!
    originalFilename: String!
    fileSize:         Int!
    mimeType:         String
    comment:          String
    downloadUrl:      String!
}

type Contact {
    id:        ID!
    user:      User!
    status:    ContactStatus!
    createdAt: DateTime!
}

type Session {
    id:         ID!
    browser:    String
    ipAddress:  String
    lastSeenAt: DateTime!
    current:    Boolean!
}

type AuthPayload {
    token: String!
    user:  User!
}

type MessagePage {
    items:      [Message!]!
    nextCursor: String
}

type RoomPage {
    items:      [Room!]!
    totalCount: Int!
}

# ── Subscription event wrappers ────────────────────────────────────────
type MessageEvent {
    type:    EventType!
    message: Message!
}

type PresenceEvent {
    userId:   ID!
    username: String!
    status:   PresenceStatus!
}

type NotificationEvent {
    type:    String!    # FRIEND_REQUEST | ROOM_INVITE | UNREAD_UPDATE
    payload: String!    # JSON-encoded payload
}

# ── Queries ────────────────────────────────────────────────────────────
type Query {
    # Users
    me:                    User!
    user(username: String!): User

    # Sessions
    mySessions:            [Session!]!

    # Rooms
    rooms(query: String, page: Int, size: Int): RoomPage!
    room(id: ID!):         Room
    roomMembers(roomId: ID!): [RoomMember!]!
    roomBans(roomId: ID!): [User!]!

    # Messages
    roomMessages(roomId: ID!, before: String, limit: Int): MessagePage!
    personalMessages(userId: ID!, before: String, limit: Int): MessagePage!

    # Contacts
    contacts:              [Contact!]!
    pendingRequests:       [Contact!]!
}

# ── Mutations ─────────────────────────────────────────────────────────
type Mutation {
    # Auth
    register(email: String!, password: String!, username: String!): AuthPayload!
    login(email: String!, password: String!): AuthPayload!
    logout:                Boolean!
    requestPasswordReset(email: String!): Boolean!
    confirmPasswordReset(token: String!, newPassword: String!): Boolean!
    changePassword(currentPassword: String!, newPassword: String!): Boolean!
    deleteAccount:         Boolean!

    # Sessions
    revokeSession(sessionId: ID!): Boolean!

    # Rooms
    createRoom(name: String!, description: String, visibility: RoomVisibility!): Room!
    updateRoom(id: ID!, name: String, description: String, visibility: RoomVisibility): Room!
    deleteRoom(id: ID!):   Boolean!
    joinRoom(id: ID!):     Boolean!
    leaveRoom(id: ID!):    Boolean!
    inviteToRoom(roomId: ID!, username: String!): Boolean!
    promoteAdmin(roomId: ID!, userId: ID!): Boolean!
    demoteAdmin(roomId: ID!, userId: ID!): Boolean!
    kickMember(roomId: ID!, userId: ID!): Boolean!
    banFromRoom(roomId: ID!, userId: ID!): Boolean!
    unbanFromRoom(roomId: ID!, userId: ID!): Boolean!

    # Messages
    sendRoomMessage(roomId: ID!, content: String!, replyToId: ID, attachmentIds: [ID!]): Message!
    sendPersonalMessage(userId: ID!, content: String!, replyToId: ID, attachmentIds: [ID!]): Message!
    editMessage(id: ID!, content: String!): Message!
    deleteMessage(id: ID!): Boolean!

    # Contacts
    sendFriendRequest(username: String!, message: String): Contact!
    acceptFriendRequest(requestId: ID!): Contact!
    declineFriendRequest(requestId: ID!): Boolean!
    removeFriend(userId: ID!): Boolean!
    banUser(userId: ID!): Boolean!
    unbanUser(userId: ID!): Boolean!
}

# ── Subscriptions ──────────────────────────────────────────────────────
type Subscription {
    # Real-time messages in a room
    roomMessages(roomId: ID!): MessageEvent!

    # Real-time personal messages
    personalMessages: MessageEvent!

    # Presence changes for members of a room
    roomPresence(roomId: ID!): PresenceEvent!

    # Notifications for the authenticated user
    notifications: NotificationEvent!
}
```

---

## 8. Milestones

### Milestone 1 — Project Foundation & Auth
**Deliverables:**
- Maven project with all dependencies (pom.xml), Docker Compose (`app`, `postgres`, `kafka`, `zookeeper`).
- Flyway migrations for the full DB schema (all tables from §6).
- Spring Security config: JWT filter, `UserDetailsServiceImpl`.
- Auth endpoints: register, login, logout, password reset (§3.1).
- `GET /users/me`, `PUT /users/me/password`, `DELETE /users/me` (§3.2).
- Session management: `GET /sessions`, `DELETE /sessions/{id}` (§3.3).
- Unit tests for `AuthService`.

### Milestone 2 — Rooms & Membership
**Deliverables:**
- Full room lifecycle: create, read, update, delete (§3.5).
- Room member management: join/leave, invite (private rooms), promote/demote admin, kick, ban/unban (§3.6).
- Public room catalog with search and pagination.
- Access-control guards: owner-only, admin-only, ban checks.
- `GET /users/{username}` (§3.2).
- Integration tests for room service.

### Milestone 3 — Real-time Messaging
**Deliverables:**
- WebSocket STOMP config (`/ws` endpoint, SockJS fallback).
- `StompEventListener` → publishes `PresenceEvent` to `presence.events` Kafka topic on connect/disconnect.
- Kafka producer/consumer for `chat.messages`: messages written to Kafka on send, consumer fans out to WebSocket subscribers.
- Room messaging: send, edit, delete (§3.7 + WebSocket).
- Personal messaging (friends only, neither banned): send, history (§3.7).
- Attachment upload/download REST endpoints (§3.8); local filesystem storage; access control.
- Message pagination (cursor-based, `before` + `limit`).
- Integration tests for messaging flow.

### Milestone 4 — Presence & Notifications
**Deliverables:**
- Kafka Streams topology (`PresenceStateTopology`): aggregates `presence.events` → `presence.state` KTable.
- AFK timer: heartbeat handler marks session AFK after 60 s of silence; publishes AFK event.
- Multi-tab handling: tab counter per user; offline only when count reaches 0.
- `presence.state` consumer pushes updates to `/topic/rooms/{roomId}/presence` within 2 s.
- `notifications` Kafka topic + consumer → `/user/queue/notifications` (friend requests, room invites, unread counters).
- Unread indicator counts tracked in DB per user per chat.
- Integration tests for presence state machine.

### Milestone 5 — Contacts & Access Control Hardening
**Deliverables:**
- Full contacts/friends API (§3.4): request, accept, decline, cancel, remove, user-ban, unban.
- Guard: personal message send fails if not friends or either party has banned the other.
- Guard: room message history and files inaccessible after room ban.
- Guard: attachment download returns 403 if requester is no longer a member/participant.
- Account deletion cascade: deletes owned rooms, all messages, all attachments, removes memberships.
- Caffeine caches: room membership, user lookups, presence state, session validation.
- Integration tests for contact and ban scenarios.

### Milestone 6 — GraphQL, Polish & Packaging
**Deliverables:**
- GraphQL schema wired (`schema.graphqls`), all Query + Mutation resolvers backed by existing services.
- GraphQL Subscriptions over WebSocket (`roomMessages`, `personalMessages`, `roomPresence`, `notifications`).
- SpringDoc / Swagger UI enabled at `/swagger-ui.html`.
- `Dockerfile` (multi-stage: build → runtime); `docker-compose.yml` with health checks.
- End-to-end smoke tests: register → login → create room → send message → receive via subscription → upload file → download file.
- `README.md` with build, run, and test instructions.
