# TODO — Milestone 9: Contacts, Admin, Profile & Polish

---

## 1. API Layer — Contacts, Sessions, Admin (`src/api/`)

### `contacts.ts`
- [x] `getFriends()` → `GET /contacts` → `Contact[]`
- [x] `getIncomingRequests()` → `GET /contacts/requests/incoming` → `Contact[]`
- [x] `sendFriendRequest(targetUsername, message?)` → `POST /contacts/requests` → `Contact`
- [x] `acceptFriendRequest(requestId)` → `PUT /contacts/requests/{requestId}/accept` → `Contact`
- [x] `declineFriendRequest(requestId)` → `DELETE /contacts/requests/{requestId}`
- [x] `removeFriend(userId)` → `DELETE /contacts/{userId}`
- [x] `banUser(userId)` → `POST /contacts/{userId}/ban`
- [x] `unbanUser(userId)` → `DELETE /contacts/{userId}/ban`

### `sessions.ts`
- [x] `getActiveSessions()` → `GET /sessions` → `Session[]`
- [x] `revokeSession(sessionId)` → `DELETE /sessions/{sessionId}`

### `rooms.ts` additions
- [x] `updateRoom(roomId, name?, description?, visibility?)` → `PUT /rooms/{roomId}` → `Room`
- [x] `deleteRoom(roomId)` → `DELETE /rooms/{roomId}`
- [x] `inviteUser(roomId, username)` → `POST /rooms/{roomId}/invites`
- [x] `promoteAdmin(roomId, userId)` → `POST /rooms/{roomId}/admins/{userId}`
- [x] `demoteAdmin(roomId, userId)` → `DELETE /rooms/{roomId}/admins/{userId}`
- [x] `kickMember(roomId, userId)` → `POST /rooms/{roomId}/members/{userId}/kick`
- [x] `getBans(roomId)` → `GET /rooms/{roomId}/bans` → `BanResponse[]`
- [x] `banMember(roomId, userId)` → `POST /rooms/{roomId}/bans/{userId}`
- [x] `unbanMember(roomId, userId)` → `DELETE /rooms/{roomId}/bans/{userId}`

### `users.ts` additions
- [x] `changePassword(currentPassword, newPassword)` → `PUT /users/me/password`
- [x] `deleteAccount()` → `DELETE /users/me`

### `messages.ts` additions
- [x] `getPersonalMessages(userId, before?, limit?)` → `GET /chats/{userId}/messages` → `Message[]`
- [x] `sendPersonalMessage(userId, content, replyToId?, attachmentIds?)` → `POST /chats/{userId}/messages` → `Message` (if REST endpoint exists; otherwise send via WebSocket `/app/chats/{userId}/send`)

### `notifications.ts` additions
- [x] `markPersonalRead(userId)` → `POST /chats/{userId}/messages/read`

---

## 2. Additional Types (`src/types/api.ts`)

- [x] `BanResponse`: `user: User`, `bannedBy: User`, `bannedAt: string`

---

## 3. Zustand Store — Contacts (`src/stores/useContactStore.ts`)

- [x] State: `friends: Contact[]`, `incomingRequests: Contact[]`, `selectedContactId: number | null`, `loading: boolean`
- [x] Actions:
  - `fetchFriends()` — calls API, sets friends
  - `fetchIncomingRequests()` — calls API, sets incomingRequests
  - `sendRequest(targetUsername, message?)` — calls API, toast, refresh
  - `acceptRequest(requestId)` — calls API, move from requests to friends
  - `declineRequest(requestId)` — calls API, remove from requests
  - `removeFriend(userId)` — calls API, remove from friends
  - `banUser(userId)` — calls API, remove from friends
  - `unbanUser(userId)` — calls API
  - `selectContact(id)` — sets selectedContactId

---

## 4. Contacts — Sidebar & Personal Chat

### Sidebar contacts section (`src/components/layout/Sidebar.tsx` update)
- [x] Below rooms: "Contacts" section showing accepted friends
- [x] Each contact shows: username, online status dot, unread badge
- [x] Clicking a contact: `selectContact(id)`, clear room selection, mark personal read
- [x] "Add Friend" button → opens friend request modal

### Friend request modal (`src/components/contact/FriendRequestModal.tsx`)
- [x] Input: target username, optional message
- [x] "Send Request" button → calls `sendRequest()`
- [x] Success toast

### Incoming requests view (`src/components/contact/IncomingRequests.tsx`)
- [x] Accessible from sidebar or TopNav "Contacts" button
- [x] List of pending requests showing: username, message, date, "Accept" / "Decline" buttons
- [x] Accept → toast, contact appears in friends list
- [x] Decline → toast, request removed

### Personal chat view (`src/components/layout/ChatArea.tsx` update)
- [x] When `selectedContactId` is set (and no room): show personal chat
- [x] Header: contact username + online status
- [x] Message list: fetch via `getPersonalMessages(userId, before, limit)`
- [x] Message input: send via REST or WebSocket
- [x] Real-time: subscribe to `/user/queue/messages` for incoming personal messages

---

## 5. Manage Room Modal (`src/components/room/ManageRoomModal.tsx`)

### Tab structure
- [x] 5-tab layout using a tab bar: Members | Admins | Banned | Invitations | Settings

### Members tab
- [x] Searchable list of room members
- [x] Each row: username, role badge, presence dot
- [x] Actions (visible to owner/admin): "Make Admin", "Ban", "Kick" buttons
- [x] Actions call: `promoteAdmin()`, `banMember()`, `kickMember()` → refresh members

### Admins tab
- [x] List of members with ADMIN or OWNER role
- [x] "Remove Admin" button (demote) for admins (owner cannot be removed)
- [x] Calls `demoteAdmin()` → refresh members

### Banned Users tab
- [x] Fetch via `getBans(roomId)` → list of `BanResponse`
- [x] Each row: username, banned-by, date
- [x] "Unban" button → calls `unbanMember()` → refresh

### Invitations tab (private rooms)
- [x] Username input + "Send Invite" button
- [x] Calls `inviteUser(roomId, username)` → toast

### Settings tab
- [x] Editable: room name, description, visibility (public/private radio)
- [x] "Save Changes" button → calls `updateRoom()` → toast, refresh room
- [x] "Delete Room" button → confirmation dialog → calls `deleteRoom()` → toast, deselect room, refresh rooms

### Access control
- [x] Show "Manage Room" button in `RightPanel` only for owner/admin
- [x] Settings tab "Delete Room" only for owner

---

## 6. Message Actions — Edit & Delete

### Update `MessageBubble.tsx`
- [x] Hover actions for own messages: "Edit", "Delete" buttons (in addition to "Reply")
- [x] Admin/owner sees "Delete" on any message in the room
- [x] Edit: opens inline edit textarea → "Save" / "Cancel" → calls `editMessage()` → updates message in store
- [x] Delete: confirmation → calls `deleteMessage()` → removes from store

---

## 7. Profile View (`src/pages/ProfilePage.tsx` or modal)

- [x] Display name: editable text input → "Save" → calls `updateProfile()`
- [x] Username: read-only field
- [x] Email: read-only field (fetched from user data)
- [x] Change password form: current password, new password, confirm new password → calls `changePassword()`
- [x] "Delete Account" button → confirmation dialog ("This action is irreversible") → calls `deleteAccount()` → clears auth → redirect to `/`
- [x] Accessible from TopNav profile dropdown

---

## 8. Sessions View (`src/pages/SessionsPage.tsx` or modal)

- [x] Fetch active sessions via `getActiveSessions()`
- [x] Table/list: browser, IP address, last seen (formatted), "Current" badge
- [x] "Revoke" button per session (not on current session)
- [x] Calls `revokeSession(sessionId)` → toast, refresh list
- [x] Accessible from TopNav "Sessions" button

---

## 9. Notification Toasts

### Friend request notifications
- [x] On `/user/queue/notifications` event with type `FRIEND_REQUEST` → show toast: "New friend request from {username}"
- [x] Clicking toast → opens incoming requests view

### Unread updates
- [x] Already handled in `useWebSocket` → increment unread count
- [x] Verify it works for both room and personal chat unreads

---

## 10. Responsive Layout

### Desktop (≥ 1024px)
- [x] Full 3-column layout (already implemented)

### Tablet (768–1023px)
- [x] Left sidebar collapsible via hamburger menu button in TopNav
- [x] Right panel hidden by default, toggle button to show/hide
- [x] Chat area takes full remaining width

### Mobile (< 768px)
- [x] Single-column layout
- [x] Sidebar as slide-out drawer (opened via hamburger)
- [x] Right panel as slide-out drawer (opened via info button)
- [x] Bottom navigation bar: Rooms, Contacts, Profile icons

---

## 11. Loading States, Error Handling & Empty States

- [x] Loading spinners: room list, message list, member list, sessions list, contacts list
- [x] Error toasts: on API failures (already partially done via try/catch + toast.error)
- [x] Empty states:
  - No rooms: "No rooms yet. Create one!"
  - No messages: "No messages yet. Say hello!"
  - No contacts: "No friends yet. Send a friend request!"
  - No incoming requests: "No pending requests"
  - No banned users: "No banned users"
  - No sessions: (should always have at least current session)

---

## 12. TopNav Updates

- [x] "Contacts" button → toggles contact view / opens incoming requests
- [x] "Sessions" button → opens sessions view/modal
- [x] "Profile" in dropdown → opens profile view/modal
- [x] Active state indicator on current nav item

---

## 13. Verification

- [x] `npm run build` — production build succeeds
- [x] Contacts: send friend request, accept, see in contacts list, chat personally
- [x] Contacts: decline request, remove friend, ban/unban user
- [x] Personal chat: send/receive messages in real time
- [x] Manage Room: promote/demote admin, kick/ban/unban member, invite user
- [x] Manage Room: edit name/description/visibility, delete room
- [x] Message actions: edit own message (shows "edited"), delete own message
- [x] Admin can delete any message in their room
- [x] Profile: edit display name, change password, delete account
- [x] Sessions: list sessions, revoke non-current session
- [x] Responsive: sidebar collapses on tablet, single-column on mobile
- [x] Friend request toast notification appears in real time
- [x] Unread badges work for both rooms and personal chats
