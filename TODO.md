# TODO — Milestone 5: Contacts & Access Control Hardening

---

## 1. DTOs

### Request DTOs (`dto/request/`)
- [x] `SendFriendRequestDto`:
  - `targetUsername` (`@NotBlank`)
  - `message` (`@Size(max=255)`, optional)

### Response DTOs (`dto/response/`)
- [x] `ContactResponse`:
  - `id` (`Long`), `user` (`UserResponse`), `status` (`ContactStatus`), `message` (`String`), `createdAt` (`OffsetDateTime`)

### EntityMapper updates (`util/EntityMapper.java`)
- [x] Add `toContactResponse(Contact contact, Long requesterId)` — show the *other* user (requester or addressee, whichever is not the requesterId)

---

## 2. Repository additions

- [x] `ContactRepository` — add:
  - `@Query` method `findFriends(Long userId)` → `List<Contact>` — all contacts with `ACCEPTED` status where user is requester OR addressee
  - `void deleteByRequesterIdAndAddresseeIdOrRequesterIdAndAddresseeId(...)` — or use `@Modifying @Query` to delete contact between two users regardless of direction
- [x] `UserBanRepository` — add:
  - `Optional<UserBan> findByBannerIdAndBannedId(Long bannerId, Long bannedId)`
  - `void deleteByBannerIdAndBannedId(Long bannerId, Long bannedId)`

---

## 3. Service Layer

### ContactService interface (`service/ContactService.java`)
- [x] Define all methods:
  - `List<ContactResponse> getFriends(Long userId)`
  - `List<ContactResponse> getIncomingRequests(Long userId)`
  - `ContactResponse sendFriendRequest(Long userId, SendFriendRequestDto request)`
  - `ContactResponse acceptFriendRequest(Long userId, Long requestId)`
  - `void declineFriendRequest(Long userId, Long requestId)`
  - `void removeFriend(Long userId, Long friendUserId)`
  - `void banUser(Long userId, Long targetUserId)`
  - `void unbanUser(Long userId, Long targetUserId)`

### ContactServiceImpl (`service/ContactServiceImpl.java`)
- [x] `getFriends`:
  - Query accepted contacts where user is requester OR addressee
  - Return list of `ContactResponse` (showing the *other* user)
- [x] `getIncomingRequests`:
  - Query `contactRepository.findByAddresseeIdAndStatus(userId, PENDING)`
  - Return list of `ContactResponse`
- [x] `sendFriendRequest`:
  - Resolve target user by username → `ResourceNotFoundException`
  - Cannot send to self → `ForbiddenException`
  - Check if already friends (either direction) → `ConflictException`
  - Check if request already exists (either direction) → `ConflictException`
  - Check if target has banned requester → `ForbiddenException`
  - Save `Contact(requester=user, addressee=target, status=PENDING, message=...)`
  - Publish `NotificationEvent(type=FRIEND_REQUEST)` to target
  - Return `ContactResponse`
- [x] `acceptFriendRequest`:
  - Load contact by requestId → `ResourceNotFoundException`
  - Verify current user is the addressee → `ForbiddenException`
  - Verify status is `PENDING` → `ConflictException` if already accepted
  - Update status to `ACCEPTED`, set `updatedAt`
  - Return `ContactResponse`
- [x] `declineFriendRequest`:
  - Load contact by requestId → `ResourceNotFoundException`
  - Verify current user is the addressee OR the requester (cancel own request) → `ForbiddenException`
  - Delete the contact record
- [x] `removeFriend`:
  - Find accepted contact between userId and friendUserId (either direction) → `ResourceNotFoundException`
  - Delete the contact record
- [x] `banUser`:
  - Cannot ban self → `ForbiddenException`
  - Check if already banned → idempotent, return silently
  - Save `UserBan(banner=user, banned=target)`
  - Remove friendship if exists (delete Contact record between them)
  - Delete any pending friend requests between them
- [x] `unbanUser`:
  - Find `UserBan` by bannerId and bannedId → no-op if not found
  - Delete the ban record

---

## 4. REST Controller

### ContactController (`rest/ContactController.java`) — `/api/contacts`
- [x] `GET /api/contacts` → `getFriends`; returns 200 + `List<ContactResponse>`
- [x] `POST /api/contacts/requests` → `sendFriendRequest`; `@Valid`; returns 201 + `ContactResponse`
- [x] `GET /api/contacts/requests/incoming` → `getIncomingRequests`; returns 200 + `List<ContactResponse>`
- [x] `PUT /api/contacts/requests/{requestId}/accept` → `acceptFriendRequest`; returns 200 + `ContactResponse`
- [x] `DELETE /api/contacts/requests/{requestId}` → `declineFriendRequest`; returns 204
- [x] `DELETE /api/contacts/{userId}` → `removeFriend`; returns 204
- [x] `POST /api/contacts/{userId}/ban` → `banUser`; returns 204
- [x] `DELETE /api/contacts/{userId}/ban` → `unbanUser`; returns 204

---

## 5. Access Control Hardening

### Room ban → message/file access denied
- [x] `MessageServiceImpl.getRoomMessages`:
  - Add check: if user is banned from the room (`roomBanRepository.existsByIdRoomIdAndIdUserId`) → `ForbiddenException`
- [x] `AttachmentServiceImpl.checkDownloadAccess`:
  - For room attachments: in addition to membership check, also check room ban → `ForbiddenException`

### Personal message guards (already implemented in M3 — verify)
- [x] Confirm `sendPersonalMessage` checks friendship + both-direction ban ✓
- [x] Confirm `sendPersonalMessage` fails if either user has banned the other ✓

---

## 6. Account Deletion Cascade

### Update `AuthServiceImpl.deleteAccount(Long userId)`
Replace soft-delete with full cascade:
- [x] Delete all attachments uploaded by user (remove files from filesystem + DB records)
- [x] Delete all messages sent by user
- [x] Delete all rooms owned by user:
  - For each owned room: delete room attachments, room messages, then room (FK cascades members, bans, invites)
- [x] Remove user from all room memberships (`RoomMemberRepository.deleteAll(findByIdUserId)`)
- [x] Delete all room bans involving user (as banned user)
- [x] Delete all contacts involving user (both as requester and addressee)
- [x] Delete all user bans involving user (both as banner and banned)
- [x] Delete all unread counts involving user
- [x] Revoke all sessions (`UserSessionRepository` — delete all for user)
- [x] Delete the user record from DB

### New repositories/methods needed for cascade
- [x] `AttachmentRepository` — add `List<Attachment> findByUploaderId(Long uploaderId)`
- [x] `MessageRepository` — add `@Modifying @Query` `deleteAllBySenderId(Long senderId)`
- [x] `RoomRepository` — add `List<Room> findByOwnerId(Long ownerId)`
- [x] `RoomBanRepository` — add `void deleteByIdUserId(Long userId)` (remove user from all room bans)
- [x] `ContactRepository` — add `@Modifying @Query` `deleteAllByRequesterIdOrAddresseeId(Long id1, Long id2)`
- [x] `UserBanRepository` — add `@Modifying @Query` `deleteAllByBannerIdOrBannedId(Long id1, Long id2)`
- [x] `UnreadCountRepository` — add `void deleteByUserId(Long userId)`

---

## 7. Caffeine Caching

### Update `CacheConfig.java`
- [x] Add `"presence"` cache name to the manager

### Add `@Cacheable` / `@CacheEvict` annotations
- [x] `EntityLoaderService.loadActiveUser` → `@Cacheable("users")`
- [x] `EntityLoaderService.loadRoom` → `@Cacheable("roomMembers")` (or a new `"rooms"` cache)
- [x] `RoomServiceImpl`:
  - `createRoom` → `@CacheEvict("roomMembers")` (new member added)
  - `joinRoom`, `leaveRoom`, `kickMember`, `banMember` → evict `roomMembers` for the room
- [x] `ContactService`:
  - `sendFriendRequest`, `acceptFriendRequest`, `removeFriend`, `banUser` → evict relevant user entries
- [x] `AuthServiceImpl`:
  - `deleteAccount` → evict user from `"users"` cache

---

## 8. Integration Tests

### `src/test/java/com/chitchat/app/service/ContactServiceImplTest.java`
- [x] `sendFriendRequest_success` — contact saved with PENDING status
- [x] `sendFriendRequest_toSelf` → `ForbiddenException`
- [x] `sendFriendRequest_alreadyFriends` → `ConflictException`
- [x] `sendFriendRequest_alreadyPending` → `ConflictException`
- [x] `sendFriendRequest_targetBannedRequester` → `ForbiddenException`
- [x] `acceptFriendRequest_success` — status updated to ACCEPTED
- [x] `acceptFriendRequest_notAddressee` → `ForbiddenException`
- [x] `acceptFriendRequest_alreadyAccepted` → `ConflictException`
- [x] `declineFriendRequest_byAddressee_success`
- [x] `declineFriendRequest_byRequester_success` (cancel own)
- [x] `declineFriendRequest_byThirdParty` → `ForbiddenException`
- [x] `removeFriend_success`
- [x] `removeFriend_notFriends` → `ResourceNotFoundException`
- [x] `banUser_success_removesFriendship`
- [x] `banUser_alreadyBanned_idempotent`
- [x] `unbanUser_success`
- [x] `getFriends_returnsBothDirections`
- [x] `getIncomingRequests_returnsPendingOnly`

### Access control tests (add to `MessageServiceImplTest.java`)
- [x] `getRoomMessages_bannedUser` → `ForbiddenException`

---

## 9. Smoke Test Checklist (Manual Verification)

- [x] `POST /api/contacts/requests` — 201; friend request created
- [x] `GET /api/contacts/requests/incoming` — returns pending requests for addressee
- [x] `PUT /api/contacts/requests/{id}/accept` — 200; status changes to ACCEPTED
- [x] `GET /api/contacts` — returns accepted friends
- [x] `DELETE /api/contacts/requests/{id}` — 204; request declined/cancelled
- [x] `DELETE /api/contacts/{userId}` — 204; friendship removed
- [x] `POST /api/contacts/{userId}/ban` — 204; friendship removed, messages blocked
- [x] `DELETE /api/contacts/{userId}/ban` — 204; ban lifted
- [x] Banned user tries to send personal message → 403
- [x] Banned-from-room user tries `GET /api/rooms/{roomId}/messages` → 403
- [x] Banned-from-room user tries `GET /api/attachments/{id}` (room file) → 403
- [x] `DELETE /api/users/me` — full cascade: rooms, messages, files deleted
- [x] `mvn test` — all tests pass
