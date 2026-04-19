# TODO — Milestone 8: Rooms, Messages & Real-Time

---

## 1. Install Additional Dependencies

- [x] `npm install @stomp/stompjs sockjs-client`
- [x] `npm install -D @types/sockjs-client`
- [x] `npm install emoji-mart @emoji-mart/data @emoji-mart/react`

---

## 2. API Layer — Rooms, Messages, Attachments (`src/api/`)

### `rooms.ts`
- [x] `searchPublicRooms(query, page, size)` → `GET /rooms?q=&page=&size=` → `Page<Room>`
- [x] `createRoom(name, description, visibility)` → `POST /rooms` → `Room`
- [x] `getRoom(roomId)` → `GET /rooms/{roomId}` → `Room`
- [x] `joinRoom(roomId)` → `POST /rooms/{roomId}/join`
- [x] `leaveRoom(roomId)` → `POST /rooms/{roomId}/leave`
- [x] `getMembers(roomId)` → `GET /rooms/{roomId}/members` → `MemberResponse[]`

### `messages.ts`
- [x] `getRoomMessages(roomId, before?, limit?)` → `GET /rooms/{roomId}/messages?before=&limit=` → `Message[]`
- [x] `sendRoomMessage(roomId, content, replyToId?, attachmentIds?)` → `POST /rooms/{roomId}/messages` → `Message`
- [x] `editMessage(messageId, content)` → `PUT /messages/{messageId}` → `Message`
- [x] `deleteMessage(messageId)` → `DELETE /messages/{messageId}`

### `attachments.ts`
- [x] `uploadAttachment(file, comment?)` → `POST /attachments` (multipart/form-data) → `Attachment`
- [x] `getAttachmentUrl(attachmentId)` → returns `/api/attachments/{id}` (download URL)

### `notifications.ts`
- [x] `getUnreadCounts()` → `GET /notifications/unread` → `UnreadCount[]`
- [x] `markRoomRead(roomId)` → `POST /rooms/{roomId}/messages/read`

---

## 3. Additional Types (`src/types/`)

### `api.ts` additions
- [x] `ChatMessageEvent`: `messageId`, `chatType`, `roomId`, `senderId`, `recipientId`, `content`, `replyToId`, `attachmentIds`, `eventType` (`CREATED`|`EDITED`|`DELETED`), `createdAt`
- [x] `PresenceUpdate`: `userId`, `username`, `status` (`ONLINE`|`AFK`|`OFFLINE`)
- [x] `PageResponse<T>`: `content: T[]`, `totalElements: number`, `totalPages: number`

---

## 4. WebSocket Service (`src/api/websocket.ts`)

- [x] Create STOMP client using `@stomp/stompjs` with SockJS transport
- [x] `connect(token)` — connect to `/ws` with JWT in STOMP `Authorization` header
- [x] `disconnect()` — gracefully disconnect
- [x] `subscribe(destination, callback)` — subscribe to a STOMP destination, return unsubscribe function
- [x] `send(destination, body)` — send a STOMP message
- [x] Auto-reconnect on disconnect with exponential backoff (1s, 2s, 4s, max 30s)
- [x] Expose connection status (`connected`, `connecting`, `disconnected`)

---

## 5. Zustand Store Updates (`src/stores/`)

### `useRoomStore.ts` — full implementation
- [x] State: `rooms: Room[]`, `selectedRoomId: number | null`, `members: MemberResponse[]`, `loading: boolean`
- [x] Actions:
  - `fetchRooms()` — calls `searchPublicRooms`, sets rooms
  - `selectRoom(id)` — sets selectedRoomId, fetches members
  - `createRoom(...)` — calls API, adds to list, selects it
  - `joinRoom(id)` — calls API, refreshes rooms
  - `leaveRoom(id)` — calls API, deselects, refreshes rooms
  - `setMembers(members)` — sets member list
  - `updateMemberPresence(userId, status)` — updates a member's presence status

### `useMessageStore.ts` — full implementation
- [x] State: `messages: Message[]`, `loading: boolean`, `hasMore: boolean`, `replyTo: Message | null`
- [x] Actions:
  - `fetchMessages(roomId, before?)` — calls API, prepends or sets messages, updates `hasMore`
  - `addMessage(message)` — appends new message
  - `updateMessage(message)` — replaces edited message by ID
  - `removeMessage(messageId)` — marks message as deleted (set content to null)
  - `setReplyTo(message | null)` — sets the message being replied to
  - `clearMessages()` — resets state when switching rooms

### `useUnreadStore.ts` — new
- [x] State: `unreads: Record<string, number>` (key = `room:{id}` or `chat:{id}`)
- [x] Actions:
  - `fetchUnreads()` — calls `getUnreadCounts()`, populates map
  - `increment(key)` — increment count
  - `reset(key)` — set count to 0 (on room open)
  - `getCount(key)` — returns count for a key

---

## 6. WebSocket Hook (`src/hooks/useWebSocket.ts`)

- [x] Custom hook that connects STOMP on mount (when authenticated), disconnects on unmount
- [x] On connect: subscribe to `/user/queue/notifications` for unread updates
- [x] Sends heartbeat `/app/presence/heartbeat` every 30 seconds
- [x] Exposes `subscribe(destination, callback)` for components to use
- [x] Uses `useAuthStore` token for connection

---

## 7. Sidebar — Room List (`src/components/layout/Sidebar.tsx`)

### Replace placeholder with functional room list
- [x] On mount: fetch rooms via `useRoomStore.fetchRooms()`
- [x] Display rooms grouped: "Public Rooms" section, "Private Rooms" section
- [x] Each room item shows: hash icon, room name, member count, unread badge (from `useUnreadStore`)
- [x] Clicking a room: `selectRoom(id)`, `markRoomRead(roomId)`, reset unread
- [x] Selected room highlighted with active background
- [x] Search input filters rooms by name (client-side)
- [x] "Create Room" button at bottom opens `CreateRoomModal`

---

## 8. Create Room Modal (`src/components/room/CreateRoomModal.tsx`)

- [x] Fields: name (required), description (optional), visibility (radio: Public / Private)
- [x] "Create" button → calls `useRoomStore.createRoom()`
- [x] On success: toast, close modal, room selected
- [x] Validation: name required, max 100 chars

---

## 9. Chat Area — Message List + Input (`src/components/layout/ChatArea.tsx`)

### Replace placeholder with functional chat view

#### Room Header (`src/components/chat/RoomHeader.tsx`)
- [x] Shows room name, description, visibility badge
- [x] "Join" button if not a member (public rooms) / "Leave" button if member (non-owner)

#### Message List (`src/components/chat/MessageList.tsx`)
- [x] Renders list of messages in chronological order (oldest at top, newest at bottom)
- [x] Each message renders via `MessageBubble` component
- [x] Infinite scroll: on scroll to top, fetch older messages via `fetchMessages(roomId, oldestMessageCreatedAt)`
- [x] Loading spinner at top while fetching
- [x] Auto-scroll to bottom on new message **only if** user is at bottom
- [x] "N new messages ↓" badge when new messages arrive while scrolled up — clicking scrolls to bottom

#### MessageBubble (`src/components/chat/MessageBubble.tsx`)
- [x] Shows: sender username, timestamp (via `date-fns format`), message content
- [x] Reply quote: if `replyTo` exists, show quoted preview (username + truncated content) above message
- [x] Attachments: images shown inline (`<img>`), files shown as download link with filename + size
- [x] "Edited" indicator if `editedAt` is set
- [x] Deleted messages: show "[message deleted]" in italic gray
- [x] Hover actions: "Reply" button (sets `replyTo` in store)

#### Message Input (`src/components/chat/MessageInput.tsx`)
- [x] Multiline `<textarea>` with auto-resize
- [x] Send button (or Enter key, Shift+Enter for newline)
- [x] Emoji picker button → opens `emoji-mart` Picker → inserts emoji at cursor
- [x] Attach button → file picker → uploads via `uploadAttachment()` → shows filename preview, removable
- [x] Reply indicator: when `replyTo` is set, shows quoted preview above input with cancel (X) button
- [x] On send: calls `sendRoomMessage(roomId, content, replyToId, attachmentIds)` → clears input + replyTo + attachments

---

## 10. Right Panel — Room Info & Members (`src/components/layout/RightPanel.tsx`)

### Replace placeholder with functional panel
- [x] Room info header: room name, visibility badge, owner username
- [x] Member list: fetch via `useRoomStore` members
- [x] Each member shows: username, role badge (Owner/Admin/Member), presence dot (green=online, yellow=AFK, gray=offline)
- [x] Subscribe to `/topic/rooms/{roomId}/presence` for real-time presence updates → `updateMemberPresence()`

---

## 11. Real-Time Integration

### Room message subscription
- [x] When a room is selected: subscribe to `/topic/rooms/{roomId}`
- [x] On `ChatMessageEvent` received:
  - `CREATED` → `addMessage(...)` (map event to Message type, or re-fetch the message)
  - `EDITED` → `updateMessage(...)`
  - `DELETED` → `removeMessage(messageId)`
- [x] When room changes: unsubscribe from previous room, subscribe to new

### Presence subscription
- [x] When a room is selected: subscribe to `/topic/rooms/{roomId}/presence`
- [x] On `PresenceUpdate` received → `updateMemberPresence(userId, status)`
- [x] When room changes: unsubscribe previous, subscribe new

### Unread updates
- [x] Subscribe to `/user/queue/notifications` (done in `useWebSocket` hook)
- [x] On `UNREAD_UPDATE` event → `useUnreadStore.increment(key)`

### Heartbeat
- [x] Send `/app/presence/heartbeat` every 30s (done in `useWebSocket` hook)

---

## 12. Wire Up ChatPage

### Update `ChatPage.tsx`
- [x] Initialize `useWebSocket` hook
- [x] Fetch rooms on mount
- [x] Fetch unreads on mount
- [x] Pass `selectedRoomId` to conditionally render `ChatArea` content vs welcome placeholder
- [x] When `selectedRoomId` changes: fetch messages, fetch members, subscribe to room topics

---

## 13. Verification

- [x] `npm run build` — production build succeeds
- [x] Landing page → Register → redirects to `/chat` with 3-column layout
- [x] Sidebar shows rooms fetched from API
- [x] "Create Room" modal creates a room, appears in sidebar
- [x] Clicking a room loads message history in center area
- [x] Typing and sending a message appears in the message list
- [x] Opening a second browser tab: message sent in one tab appears in the other in real time
- [x] Scrolling up loads older messages (infinite scroll)
- [x] Reply-to: clicking Reply shows quote, sends with replyToId
- [x] Emoji picker inserts emoji into message
- [x] File attach uploads file, message shows attachment link
- [x] Right panel shows room members with presence dots
- [x] Unread badges appear on rooms with new messages
- [x] Heartbeat keeps connection alive
