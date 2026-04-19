package com.chitchat.app.service;

import com.chitchat.app.dao.AttachmentRepository;
import com.chitchat.app.dao.MessageRepository;
import com.chitchat.app.dao.RoomBanRepository;
import com.chitchat.app.dao.RoomInviteRepository;
import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.dao.RoomRepository;
import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dto.request.CreateRoomRequest;
import com.chitchat.app.dto.request.InviteToRoomRequest;
import com.chitchat.app.dto.request.UpdateRoomRequest;
import com.chitchat.app.dto.response.BanResponse;
import com.chitchat.app.dto.response.MemberResponse;
import com.chitchat.app.dto.response.RoomResponse;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.RoomBan;
import com.chitchat.app.entity.RoomBanId;
import com.chitchat.app.entity.RoomInvite;
import com.chitchat.app.entity.RoomMember;
import com.chitchat.app.entity.RoomMemberId;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.enums.RoomRole;
import com.chitchat.app.entity.enums.RoomVisibility;
import com.chitchat.app.exception.ConflictException;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomBanRepository roomBanRepository;
    private final RoomInviteRepository roomInviteRepository;
    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final EntityLoaderService entityLoader;

    @Override
    @Transactional(readOnly = true)
    public Page<RoomResponse> searchPublicRooms(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return roomRepository
                .findByVisibilityAndNameContainingIgnoreCase(RoomVisibility.PUBLIC, query, pageable)
                .map(room -> EntityMapper.toRoomResponse(room,
                        roomMemberRepository.countByIdRoomId(room.getId())));
    }

    @Override
    public RoomResponse createRoom(Long ownerId, CreateRoomRequest request) {
        if (roomRepository.existsByName(request.getName())) {
            throw new ConflictException("Room name already taken: " + request.getName());
        }
        User owner = entityLoader.loadActiveUser(ownerId);
        Room room = Room.builder()
                .name(request.getName())
                .description(request.getDescription())
                .visibility(request.getVisibility())
                .owner(owner)
                .createdAt(OffsetDateTime.now())
                .build();
        roomRepository.save(room);

        RoomMember ownerMember = RoomMember.builder()
                .id(new RoomMemberId(room.getId(), ownerId))
                .room(room)
                .user(owner)
                .role(RoomRole.OWNER)
                .joinedAt(OffsetDateTime.now())
                .build();
        roomMemberRepository.save(ownerMember);

        return EntityMapper.toRoomResponse(room, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoom(Long roomId, Long requesterId) {
        Room room = entityLoader.loadRoom(roomId);
        if (room.getVisibility() == RoomVisibility.PRIVATE
                && !roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, requesterId)) {
            throw new ForbiddenException("Access to private room denied");
        }
        int memberCount = roomMemberRepository.countByIdRoomId(roomId);
        return EntityMapper.toRoomResponse(room, memberCount);
    }

    @Override
    @CacheEvict(value = "rooms", key = "#roomId")
    public RoomResponse updateRoom(Long roomId, Long requesterId, UpdateRoomRequest request) {
        Room room = entityLoader.loadRoom(roomId);
        requireOwner(room, requesterId);

        if (request.getName() != null && !request.getName().equals(room.getName())) {
            if (roomRepository.existsByName(request.getName())) {
                throw new ConflictException("Room name already taken: " + request.getName());
            }
            room.setName(request.getName());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }
        if (request.getVisibility() != null) {
            room.setVisibility(request.getVisibility());
        }
        roomRepository.save(room);

        int memberCount = roomMemberRepository.countByIdRoomId(roomId);
        return EntityMapper.toRoomResponse(room, memberCount);
    }

    @Override
    @CacheEvict(value = "rooms", key = "#roomId")
    public void deleteRoom(Long roomId, Long requesterId) {
        Room room = entityLoader.loadRoom(roomId);
        requireOwner(room, requesterId);

        attachmentRepository.deleteAllByMessageRoomId(roomId);
        messageRepository.deleteAllByRoomId(roomId);
        roomRepository.delete(room);
    }

    @Override
    public void joinRoom(Long roomId, Long userId) {
        Room room = entityLoader.loadRoom(roomId);
        if (room.getVisibility() != RoomVisibility.PUBLIC) {
            throw new ForbiddenException("Cannot join a private room directly");
        }
        if (roomBanRepository.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            throw new ForbiddenException("User is banned from this room");
        }
        if (roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            return;
        }
        User user = entityLoader.loadActiveUser(userId);
        RoomMember member = RoomMember.builder()
                .id(new RoomMemberId(roomId, userId))
                .room(room)
                .user(user)
                .role(RoomRole.MEMBER)
                .joinedAt(OffsetDateTime.now())
                .build();
        roomMemberRepository.save(member);
    }

    @Override
    public void leaveRoom(Long roomId, Long userId) {
        Room room = entityLoader.loadRoom(roomId);
        if (room.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Owner cannot leave their own room");
        }
        roomMemberRepository.deleteByIdRoomIdAndIdUserId(roomId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long roomId, Long requesterId) {
        Room room = entityLoader.loadRoom(roomId);
        if (room.getVisibility() == RoomVisibility.PRIVATE
                && !roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, requesterId)) {
            throw new ForbiddenException("Access to private room denied");
        }
        return roomMemberRepository.findByIdRoomId(roomId).stream()
                .map(EntityMapper::toMemberResponse)
                .toList();
    }

    @Override
    public void inviteUser(Long roomId, Long requesterId, InviteToRoomRequest request) {
        Room room = entityLoader.loadRoom(roomId);
        requireAdminOrOwner(roomId, requesterId);

        User target = userRepository.findByUsername(request.getUsername())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

        if (roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, target.getId())) {
            throw new ConflictException("User is already a member of this room");
        }
        if (roomBanRepository.existsByIdRoomIdAndIdUserId(roomId, target.getId())) {
            throw new ForbiddenException("User is banned from this room");
        }

        User inviter = entityLoader.loadActiveUser(requesterId);
        RoomInvite invite = RoomInvite.builder()
                .room(room)
                .invitedUser(target)
                .invitedBy(inviter)
                .createdAt(OffsetDateTime.now())
                .build();
        roomInviteRepository.save(invite);

        RoomMember member = RoomMember.builder()
                .id(new RoomMemberId(roomId, target.getId()))
                .room(room)
                .user(target)
                .role(RoomRole.MEMBER)
                .joinedAt(OffsetDateTime.now())
                .build();
        roomMemberRepository.save(member);

        roomInviteRepository.deleteByRoomIdAndInvitedUserId(roomId, target.getId());
    }

    @Override
    public void promoteAdmin(Long roomId, Long requesterId, Long targetUserId) {
        Room room = entityLoader.loadRoom(roomId);
        requireOwner(room, requesterId);

        RoomMember target = roomMemberRepository.findByIdRoomIdAndIdUserId(roomId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (target.getRole() != RoomRole.MEMBER) {
            throw new ForbiddenException("Only MEMBER-role users can be promoted to ADMIN");
        }
        target.setRole(RoomRole.ADMIN);
        roomMemberRepository.save(target);
    }

    @Override
    public void demoteAdmin(Long roomId, Long requesterId, Long targetUserId) {
        Room room = entityLoader.loadRoom(roomId);
        if (!requesterId.equals(targetUserId) && !room.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenException("Only the owner or the admin themselves can demote an admin");
        }
        RoomMember target = roomMemberRepository.findByIdRoomIdAndIdUserId(roomId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (target.getRole() == RoomRole.OWNER) {
            throw new ForbiddenException("Cannot demote the room owner");
        }
        target.setRole(RoomRole.MEMBER);
        roomMemberRepository.save(target);
    }

    @Override
    public void kickMember(Long roomId, Long requesterId, Long targetUserId) {
        entityLoader.loadRoom(roomId);
        RoomMember requester = requireAdminOrOwner(roomId, requesterId);

        RoomMember target = roomMemberRepository.findByIdRoomIdAndIdUserId(roomId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (target.getRole() == RoomRole.OWNER) {
            throw new ForbiddenException("Cannot kick the room owner");
        }
        if (requester.getRole() == RoomRole.ADMIN && target.getRole() == RoomRole.ADMIN) {
            throw new ForbiddenException("Admins cannot kick other admins");
        }
        roomMemberRepository.deleteByIdRoomIdAndIdUserId(roomId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BanResponse> getBans(Long roomId, Long requesterId) {
        entityLoader.loadRoom(roomId);
        requireAdminOrOwner(roomId, requesterId);
        return roomBanRepository.findByIdRoomId(roomId).stream()
                .map(EntityMapper::toBanResponse)
                .toList();
    }

    @Override
    public void banMember(Long roomId, Long requesterId, Long targetUserId) {
        Room room = entityLoader.loadRoom(roomId);
        RoomMember requester = requireAdminOrOwner(roomId, requesterId);

        if (room.getOwner().getId().equals(targetUserId)) {
            throw new ForbiddenException("Cannot ban the room owner");
        }
        roomMemberRepository.findByIdRoomIdAndIdUserId(roomId, targetUserId).ifPresent(target -> {
            if (requester.getRole() == RoomRole.ADMIN && target.getRole() == RoomRole.ADMIN) {
                throw new ForbiddenException("Admins cannot ban other admins");
            }
            roomMemberRepository.deleteByIdRoomIdAndIdUserId(roomId, targetUserId);
        });

        if (!roomBanRepository.existsByIdRoomIdAndIdUserId(roomId, targetUserId)) {
            User target = entityLoader.loadActiveUser(targetUserId);
            User banner = entityLoader.loadActiveUser(requesterId);
            RoomBan ban = RoomBan.builder()
                    .id(new RoomBanId(roomId, targetUserId))
                    .room(room)
                    .user(target)
                    .bannedBy(banner)
                    .bannedAt(OffsetDateTime.now())
                    .build();
            roomBanRepository.save(ban);
        }
    }

    @Override
    public void unbanMember(Long roomId, Long requesterId, Long targetUserId) {
        entityLoader.loadRoom(roomId);
        requireAdminOrOwner(roomId, requesterId);
        roomBanRepository.findByIdRoomIdAndIdUserId(roomId, targetUserId)
                .ifPresent(roomBanRepository::delete);
    }

    // ── Private helpers ───────────────────────────────────────────────

    private void requireOwner(Room room, Long requesterId) {
        if (!room.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenException("Only the room owner can perform this action");
        }
    }

    private RoomMember requireAdminOrOwner(Long roomId, Long requesterId) {
        RoomMember member = roomMemberRepository.findByIdRoomIdAndIdUserId(roomId, requesterId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this room"));
        if (member.getRole() == RoomRole.MEMBER) {
            throw new ForbiddenException("Insufficient permissions — admin or owner required");
        }
        return member;
    }
}
