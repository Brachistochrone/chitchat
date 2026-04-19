# TODO — Milestone 2: Rooms & Membership

---

## 1. DTOs

### Request DTOs (`dto/request/`)
- [x] `CreateRoomRequest`:
  - `name` (`@NotBlank @Size(max=100)`)
  - `description` (`@Size(max=2000)`)
  - `visibility` (`@NotNull RoomVisibility`)
- [x] `UpdateRoomRequest`:
  - `name` (`@Size(max=100)`)
  - `description` (`@Size(max=2000)`)
  - `visibility` (`RoomVisibility`)
- [x] `InviteToRoomRequest`:
  - `username` (`@NotBlank`)

### Response DTOs (`dto/response/`)
- [x] `RoomResponse`:
  - `id`, `name`, `description`, `visibility` (`RoomVisibility`), `memberCount` (`int`), `createdAt`
  - `owner` (`UserResponse`)
- [x] `MemberResponse`:
  - `user` (`UserResponse`), `role` (`RoomRole`), `joinedAt`
- [x] `BanResponse`:
  - `user` (`UserResponse`), `bannedBy` (`UserResponse`), `bannedAt`

### EntityMapper updates (`util/EntityMapper.java`)
- [x] Add `toRoomResponse(Room room, int memberCount)` static method
- [x] Add `toMemberResponse(RoomMember member)` static method
- [x] Add `toBanResponse(RoomBan ban)` static method

---

## 2. Repository additions

- [x] `RoomMemberRepository` — add query methods:
  - `Optional<RoomMember> findByIdRoomIdAndIdUserId(Long roomId, Long userId)`
  - `int countByIdRoomId(Long roomId)`
  - `void deleteByIdRoomIdAndIdUserId(Long roomId, Long userId)`
- [x] `RoomBanRepository` — add query method:
  - `Optional<RoomBan> findByIdRoomIdAndIdUserId(Long roomId, Long userId)`
- [x] `RoomInviteRepository` — add query method:
  - `Optional<RoomInvite> findByRoomIdAndInvitedUserId(Long roomId, Long userId)`
  - `void deleteByRoomIdAndInvitedUserId(Long roomId, Long userId)`
- [x] `MessageRepository` — add:
  - `void deleteAllByRoomId(Long roomId)` (for room deletion cascade)
- [x] `AttachmentRepository` — add:
  - `void deleteAllByMessageRoomId(Long roomId)` (for room deletion cascade)

---

## 3. Service Layer

### RoomService interface (`service/RoomService.java`)
- [x] Define all methods:
  - `Page<RoomResponse> searchPublicRooms(String query, int page, int size)`
  - `RoomResponse createRoom(Long ownerId, CreateRoomRequest request)`
  - `RoomResponse getRoom(Long roomId, Long requesterId)`
  - `RoomResponse updateRoom(Long roomId, Long requesterId, UpdateRoomRequest request)`
  - `void deleteRoom(Long roomId, Long requesterId)`
  - `void joinRoom(Long roomId, Long userId)`
  - `void leaveRoom(Long roomId, Long userId)`
  - `List<MemberResponse> getMembers(Long roomId, Long requesterId)`
  - `void inviteUser(Long roomId, Long requesterId, String targetUsername)`
  - `void promoteAdmin(Long roomId, Long requesterId, Long targetUserId)`
  - `void demoteAdmin(Long roomId, Long requesterId, Long targetUserId)`
  - `void kickMember(Long roomId, Long requesterId, Long targetUserId)`
  - `List<BanResponse> getBans(Long roomId, Long requesterId)`
  - `void banMember(Long roomId, Long requesterId, Long targetUserId)`
  - `void unbanMember(Long roomId, Long requesterId, Long targetUserId)`

### RoomServiceImpl (`service/RoomServiceImpl.java`)
- [x] `searchPublicRooms` — query `RoomRepository` with `LIKE` on name, return paginated `RoomResponse`
- [x] `createRoom`:
  - Check name uniqueness → `ConflictException` if taken
  - Save `Room` entity with owner
  - Add owner as `RoomMember` with role `OWNER`
  - Return `RoomResponse`
- [x] `getRoom`:
  - Load room → `ResourceNotFoundException` if absent
  - Private rooms: requester must be a member → `ForbiddenException` if not
  - Return `RoomResponse` with member count
- [x] `updateRoom`:
  - Load room; verify requester is owner → `ForbiddenException`
  - If name is changing, check uniqueness → `ConflictException`
  - Update fields, save, return `RoomResponse`
- [x] `deleteRoom`:
  - Load room; verify requester is owner → `ForbiddenException`
  - Delete all attachments (filesystem + DB records) for the room's messages
  - Delete all messages for the room
  - Delete room (cascades members, bans, invites via DB FK)
- [x] `joinRoom`:
  - Load room; must be `PUBLIC` → `ForbiddenException` if private
  - User must not be banned → `ForbiddenException`
  - User must not already be a member (idempotent: silently succeed if already member)
  - Save `RoomMember` with role `MEMBER`
- [x] `leaveRoom`:
  - Load room; owner cannot leave → `ForbiddenException`
  - Remove `RoomMember` record; no-op if not a member
- [x] `getMembers`:
  - Load room; verify requester is a member (or room is public) → `ForbiddenException` if private and not member
  - Return list of `MemberResponse`
- [x] `inviteUser`:
  - Load room; verify requester is owner or admin → `ForbiddenException`
  - Resolve target by username → `ResourceNotFoundException`
  - Target must not already be a member → `ConflictException`
  - Target must not be banned → `ForbiddenException`
  - Save `RoomInvite`; also immediately add them as `MEMBER` and delete invite record (accept-on-invite flow)
- [x] `promoteAdmin`:
  - Load room; verify requester is owner → `ForbiddenException`
  - Target must be a current `MEMBER` → `ResourceNotFoundException`
  - Upgrade role to `ADMIN`, save
- [x] `demoteAdmin`:
  - Load room; verify requester is owner OR requester is the same user (self-demotion) → `ForbiddenException`
  - Target must not be the owner → `ForbiddenException`
  - Downgrade role to `MEMBER`, save
- [x] `kickMember`:
  - Load room; verify requester is admin or owner → `ForbiddenException`
  - Target must not be the owner → `ForbiddenException`
  - Admins cannot kick other admins unless requester is owner
  - Remove `RoomMember` record
- [x] `getBans`:
  - Load room; verify requester is admin or owner → `ForbiddenException`
  - Return list of `BanResponse`
- [x] `banMember`:
  - Load room; verify requester is admin or owner → `ForbiddenException`
  - Target must not be the owner → `ForbiddenException`
  - Admins cannot ban other admins unless requester is owner
  - Remove `RoomMember` record if present
  - Save `RoomBan`
- [x] `unbanMember`:
  - Load room; verify requester is admin or owner → `ForbiddenException`
  - Remove `RoomBan` record; no-op if not banned

---

## 4. REST Controllers

### RoomController (`rest/RoomController.java`) — `/api/rooms`
- [x] `GET /` → `searchPublicRooms`; query params: `q` (default `""`), `page` (default 0), `size` (default 20); returns 200 + `Page<RoomResponse>`
- [x] `POST /` → `createRoom`; `@Valid`; returns 201 + `RoomResponse`
- [x] `GET /{roomId}` → `getRoom`; returns 200 + `RoomResponse`
- [x] `PUT /{roomId}` → `updateRoom`; `@Valid`; returns 200 + `RoomResponse`
- [x] `DELETE /{roomId}` → `deleteRoom`; returns 204
- [x] `POST /{roomId}/join` → `joinRoom`; returns 204
- [x] `POST /{roomId}/leave` → `leaveRoom`; returns 204

### RoomMemberController (`rest/RoomMemberController.java`) — `/api/rooms/{roomId}`
- [x] `GET /members` → `getMembers`; returns 200 + `List<MemberResponse>`
- [x] `POST /invites` → `inviteUser`; `@Valid`; returns 204
- [x] `POST /admins/{userId}` → `promoteAdmin`; returns 204
- [x] `DELETE /admins/{userId}` → `demoteAdmin`; returns 204
- [x] `POST /members/{userId}/kick` → `kickMember`; returns 204
- [x] `GET /bans` → `getBans`; returns 200 + `List<BanResponse>`
- [x] `POST /bans/{userId}` → `banMember`; returns 204
- [x] `DELETE /bans/{userId}` → `unbanMember`; returns 204

### AppConstants updates (`util/AppConstants.java`)
- [x] Add role-check constants:
  - `ROLE_OWNER = "OWNER"`
  - `ROLE_ADMIN = "ADMIN"`
  - `ROLE_MEMBER = "MEMBER"`

---

## 5. Access-Control Guard Summary

| Action | Allowed for |
|---|---|
| Create room | Any authenticated user |
| View public room | Any authenticated user |
| View private room | Members only |
| Update room | Owner only |
| Delete room | Owner only |
| Join room | Any non-banned user (public rooms only) |
| Leave room | Any member except the owner |
| Invite user | Owner or Admin (private rooms) |
| View members | Members (or anyone for public rooms) |
| Promote to admin | Owner only |
| Demote admin | Owner, or the admin themselves (self-demotion) |
| Kick member | Admin or Owner; cannot kick owner; admin cannot kick admin |
| View ban list | Admin or Owner |
| Ban member | Admin or Owner; cannot ban owner; admin cannot ban admin |
| Unban member | Admin or Owner |

---

## 6. Integration Tests (`src/test/java/com/chitchat/app/service/RoomServiceImplTest.java`)

- [x] `createRoom_success` — room saved, owner added as OWNER member
- [x] `createRoom_duplicateName` → `ConflictException`
- [x] `getRoom_publicRoom_anyUserCanView`
- [x] `getRoom_privateRoom_nonMember` → `ForbiddenException`
- [x] `getRoom_notFound` → `ResourceNotFoundException`
- [x] `updateRoom_success_ownerOnly`
- [x] `updateRoom_notOwner` → `ForbiddenException`
- [x] `updateRoom_duplicateName` → `ConflictException`
- [x] `deleteRoom_success_cascadesMembers`
- [x] `deleteRoom_notOwner` → `ForbiddenException`
- [x] `joinRoom_success_publicRoom`
- [x] `joinRoom_privateRoom` → `ForbiddenException`
- [x] `joinRoom_bannedUser` → `ForbiddenException`
- [x] `leaveRoom_success`
- [x] `leaveRoom_owner` → `ForbiddenException`
- [x] `promoteAdmin_success_ownerOnly`
- [x] `promoteAdmin_notOwner` → `ForbiddenException`
- [x] `demoteAdmin_byOwner_success`
- [x] `demoteAdmin_selfDemotion_success`
- [x] `demoteAdmin_targetIsOwner` → `ForbiddenException`
- [x] `kickMember_byAdmin_success`
- [x] `kickMember_targetIsOwner` → `ForbiddenException`
- [x] `kickMember_adminKicksAdmin_notOwner` → `ForbiddenException`
- [x] `banMember_success_removesFromMembers`
- [x] `banMember_targetIsOwner` → `ForbiddenException`
- [x] `unbanMember_success`
- [x] `inviteUser_success_addsAsMember`
- [x] `inviteUser_targetAlreadyMember` → `ConflictException`
- [x] `inviteUser_targetBanned` → `ForbiddenException`
- [x] `getBans_byAdmin_success`
- [x] `getBans_byNonAdmin` → `ForbiddenException`

---

## 7. Smoke Test Checklist (Manual Verification)

- [x] `POST /api/rooms` — 201, room created; requester is OWNER in member list
- [x] `GET /api/rooms?q=&page=0&size=10` — returns public rooms only
- [x] `GET /api/rooms/{id}` — public room visible to any user; private room returns 403 for non-member
- [x] `PUT /api/rooms/{id}` by non-owner — 403
- [x] `POST /api/rooms/{id}/join` (public) — 204; member appears in `GET /members`
- [x] `POST /api/rooms/{id}/join` by banned user — 403
- [x] `POST /api/rooms/{id}/leave` by owner — 403
- [x] `POST /api/rooms/{id}/admins/{userId}` — 204; role updated to ADMIN
- [x] `DELETE /api/rooms/{id}/admins/{userId}` — 204; role back to MEMBER
- [x] `POST /api/rooms/{id}/bans/{userId}` — 204; user no longer in members list
- [x] `DELETE /api/rooms/{id}/bans/{userId}` — 204; user can rejoin
- [x] `POST /api/rooms/{id}/members/{userId}/kick` — 204; user removed from members
- [x] `DELETE /api/rooms/{id}` by owner — 204; room gone
- [x] `DELETE /api/rooms/{id}` by non-owner — 403
- [x] `mvn test` — all tests pass
