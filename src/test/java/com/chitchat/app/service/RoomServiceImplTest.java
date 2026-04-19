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
import com.chitchat.app.entity.RoomMember;
import com.chitchat.app.entity.RoomMemberId;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.enums.RoomRole;
import com.chitchat.app.entity.enums.RoomVisibility;
import com.chitchat.app.exception.ConflictException;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private RoomBanRepository roomBanRepository;
    @Mock private RoomInviteRepository roomInviteRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityLoaderService entityLoader;

    @InjectMocks
    private RoomServiceImpl roomService;

    private User owner;
    private User admin;
    private User member;
    private User outsider;
    private Room publicRoom;
    private Room privateRoom;
    private RoomMember ownerMember;
    private RoomMember adminMember;
    private RoomMember regularMember;

    @BeforeEach
    void setUp() {
        owner    = User.builder().id(1L).username("alice").createdAt(OffsetDateTime.now()).build();
        admin    = User.builder().id(2L).username("bob").createdAt(OffsetDateTime.now()).build();
        member   = User.builder().id(3L).username("charlie").createdAt(OffsetDateTime.now()).build();
        outsider = User.builder().id(4L).username("dave").createdAt(OffsetDateTime.now()).build();

        publicRoom = Room.builder()
                .id(10L).name("Public Room").visibility(RoomVisibility.PUBLIC)
                .owner(owner).createdAt(OffsetDateTime.now()).build();
        privateRoom = Room.builder()
                .id(11L).name("Private Room").visibility(RoomVisibility.PRIVATE)
                .owner(owner).createdAt(OffsetDateTime.now()).build();

        ownerMember = RoomMember.builder()
                .id(new RoomMemberId(10L, 1L)).room(publicRoom).user(owner)
                .role(RoomRole.OWNER).joinedAt(OffsetDateTime.now()).build();
        adminMember = RoomMember.builder()
                .id(new RoomMemberId(10L, 2L)).room(publicRoom).user(admin)
                .role(RoomRole.ADMIN).joinedAt(OffsetDateTime.now()).build();
        regularMember = RoomMember.builder()
                .id(new RoomMemberId(10L, 3L)).room(publicRoom).user(member)
                .role(RoomRole.MEMBER).joinedAt(OffsetDateTime.now()).build();
    }

    // ── createRoom ────────────────────────────────────────────────────

    @Test
    void createRoom_success() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("New Room");
        request.setVisibility(RoomVisibility.PUBLIC);

        when(roomRepository.existsByName("New Room")).thenReturn(false);
        when(entityLoader.loadActiveUser(1L)).thenReturn(owner);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> {
            Room r = i.getArgument(0);
            r.setId(10L);
            return r;
        });
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(i -> i.getArgument(0));

        RoomResponse response = roomService.createRoom(1L, request);

        assertThat(response.getName()).isEqualTo("New Room");
        assertThat(response.getMemberCount()).isEqualTo(1);
        verify(roomMemberRepository).save(any(RoomMember.class));
    }

    @Test
    void createRoom_duplicateName_throwsConflict() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("New Room");
        request.setVisibility(RoomVisibility.PUBLIC);

        when(roomRepository.existsByName("New Room")).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    // ── getRoom ───────────────────────────────────────────────────────

    @Test
    void getRoom_publicRoom_anyUserCanView() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.countByIdRoomId(10L)).thenReturn(3);

        RoomResponse response = roomService.getRoom(10L, 4L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getMemberCount()).isEqualTo(3);
    }

    @Test
    void getRoom_privateRoom_nonMember_throwsForbidden() {
        when(entityLoader.loadRoom(11L)).thenReturn(privateRoom);
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(11L, 4L)).thenReturn(false);

        assertThatThrownBy(() -> roomService.getRoom(11L, 4L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getRoom_notFound_throwsResourceNotFound() {
        when(entityLoader.loadRoom(99L)).thenThrow(new ResourceNotFoundException("Room not found"));

        assertThatThrownBy(() -> roomService.getRoom(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateRoom ────────────────────────────────────────────────────

    @Test
    void updateRoom_success_ownerOnly() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setName("Updated Room");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomRepository.existsByName("Updated Room")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomMemberRepository.countByIdRoomId(10L)).thenReturn(2);

        RoomResponse response = roomService.updateRoom(10L, 1L, request);

        assertThat(response.getName()).isEqualTo("Updated Room");
    }

    @Test
    void updateRoom_notOwner_throwsForbidden() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setName("Updated Room");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);

        assertThatThrownBy(() -> roomService.updateRoom(10L, 2L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateRoom_duplicateName_throwsConflict() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setName("Taken Room");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomRepository.existsByName("Taken Room")).thenReturn(true);

        assertThatThrownBy(() -> roomService.updateRoom(10L, 1L, request))
                .isInstanceOf(ConflictException.class);
    }

    // ── deleteRoom ────────────────────────────────────────────────────

    @Test
    void deleteRoom_success_cascadesMembers() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);

        roomService.deleteRoom(10L, 1L);

        verify(attachmentRepository).deleteAllByMessageRoomId(10L);
        verify(messageRepository).deleteAllByRoomId(10L);
        verify(roomRepository).delete(publicRoom);
    }

    @Test
    void deleteRoom_notOwner_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);

        assertThatThrownBy(() -> roomService.deleteRoom(10L, 2L))
                .isInstanceOf(ForbiddenException.class);
        verify(roomRepository, never()).delete(any());
    }

    // ── joinRoom ──────────────────────────────────────────────────────

    @Test
    void joinRoom_success_publicRoom() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomBanRepository.existsByIdRoomIdAndIdUserId(10L, 4L)).thenReturn(false);
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(10L, 4L)).thenReturn(false);
        when(entityLoader.loadActiveUser(4L)).thenReturn(outsider);
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(i -> i.getArgument(0));

        roomService.joinRoom(10L, 4L);

        verify(roomMemberRepository).save(any(RoomMember.class));
    }

    @Test
    void joinRoom_privateRoom_throwsForbidden() {
        when(entityLoader.loadRoom(11L)).thenReturn(privateRoom);

        assertThatThrownBy(() -> roomService.joinRoom(11L, 4L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void joinRoom_bannedUser_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomBanRepository.existsByIdRoomIdAndIdUserId(10L, 4L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom(10L, 4L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── leaveRoom ─────────────────────────────────────────────────────

    @Test
    void leaveRoom_success() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);

        roomService.leaveRoom(10L, 3L);

        verify(roomMemberRepository).deleteByIdRoomIdAndIdUserId(10L, 3L);
    }

    @Test
    void leaveRoom_owner_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);

        assertThatThrownBy(() -> roomService.leaveRoom(10L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── promoteAdmin ──────────────────────────────────────────────────

    @Test
    void promoteAdmin_success_ownerOnly() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 3L))
                .thenReturn(Optional.of(regularMember));
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(i -> i.getArgument(0));

        roomService.promoteAdmin(10L, 1L, 3L);

        assertThat(regularMember.getRole()).isEqualTo(RoomRole.ADMIN);
    }

    @Test
    void promoteAdmin_notOwner_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);

        assertThatThrownBy(() -> roomService.promoteAdmin(10L, 2L, 3L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── demoteAdmin ───────────────────────────────────────────────────

    @Test
    void demoteAdmin_byOwner_success() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(i -> i.getArgument(0));

        roomService.demoteAdmin(10L, 1L, 2L);

        assertThat(adminMember.getRole()).isEqualTo(RoomRole.MEMBER);
    }

    @Test
    void demoteAdmin_selfDemotion_success() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(i -> i.getArgument(0));

        roomService.demoteAdmin(10L, 2L, 2L);

        assertThat(adminMember.getRole()).isEqualTo(RoomRole.MEMBER);
    }

    @Test
    void demoteAdmin_targetIsOwner_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 1L))
                .thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> roomService.demoteAdmin(10L, 1L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── kickMember ────────────────────────────────────────────────────

    @Test
    void kickMember_byAdmin_success() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 3L))
                .thenReturn(Optional.of(regularMember));

        roomService.kickMember(10L, 2L, 3L);

        verify(roomMemberRepository).deleteByIdRoomIdAndIdUserId(10L, 3L);
    }

    @Test
    void kickMember_targetIsOwner_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 1L))
                .thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> roomService.kickMember(10L, 2L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void kickMember_adminKicksAdmin_notOwner_throwsForbidden() {
        RoomMember adminMember2 = RoomMember.builder()
                .id(new RoomMemberId(10L, 5L)).room(publicRoom)
                .user(User.builder().id(5L).username("eve").build())
                .role(RoomRole.ADMIN).joinedAt(OffsetDateTime.now()).build();

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 5L))
                .thenReturn(Optional.of(adminMember2));

        assertThatThrownBy(() -> roomService.kickMember(10L, 2L, 5L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── banMember ─────────────────────────────────────────────────────

    @Test
    void banMember_success_removesFromMembers() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 3L))
                .thenReturn(Optional.of(regularMember));
        when(roomBanRepository.existsByIdRoomIdAndIdUserId(10L, 3L)).thenReturn(false);
        when(entityLoader.loadActiveUser(3L)).thenReturn(member);
        when(entityLoader.loadActiveUser(2L)).thenReturn(admin);
        when(roomBanRepository.save(any(RoomBan.class))).thenAnswer(i -> i.getArgument(0));

        roomService.banMember(10L, 2L, 3L);

        verify(roomMemberRepository).deleteByIdRoomIdAndIdUserId(10L, 3L);
        verify(roomBanRepository).save(any(RoomBan.class));
    }

    @Test
    void banMember_targetIsOwner_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> roomService.banMember(10L, 2L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── unbanMember ───────────────────────────────────────────────────

    @Test
    void unbanMember_success() {
        RoomBan ban = RoomBan.builder()
                .id(new RoomBanId(10L, 4L)).room(publicRoom).user(outsider)
                .bannedBy(admin).bannedAt(OffsetDateTime.now()).build();

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomBanRepository.findByIdRoomIdAndIdUserId(10L, 4L))
                .thenReturn(Optional.of(ban));

        roomService.unbanMember(10L, 2L, 4L);

        verify(roomBanRepository).delete(ban);
    }

    // ── inviteUser ────────────────────────────────────────────────────

    @Test
    void inviteUser_success_addsAsMember() {
        InviteToRoomRequest request = new InviteToRoomRequest();
        request.setUsername("dave");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(userRepository.findByUsername("dave")).thenReturn(Optional.of(outsider));
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(10L, 4L)).thenReturn(false);
        when(roomBanRepository.existsByIdRoomIdAndIdUserId(10L, 4L)).thenReturn(false);
        when(entityLoader.loadActiveUser(2L)).thenReturn(admin);
        when(roomInviteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(i -> i.getArgument(0));

        roomService.inviteUser(10L, 2L, request);

        verify(roomMemberRepository).save(any(RoomMember.class));
        verify(roomInviteRepository).deleteByRoomIdAndInvitedUserId(10L, 4L);
    }

    @Test
    void inviteUser_targetAlreadyMember_throwsConflict() {
        InviteToRoomRequest request = new InviteToRoomRequest();
        request.setUsername("charlie");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(userRepository.findByUsername("charlie")).thenReturn(Optional.of(member));
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(10L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.inviteUser(10L, 2L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void inviteUser_targetBanned_throwsForbidden() {
        InviteToRoomRequest request = new InviteToRoomRequest();
        request.setUsername("dave");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(userRepository.findByUsername("dave")).thenReturn(Optional.of(outsider));
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(10L, 4L)).thenReturn(false);
        when(roomBanRepository.existsByIdRoomIdAndIdUserId(10L, 4L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.inviteUser(10L, 2L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── getBans ───────────────────────────────────────────────────────

    @Test
    void getBans_byAdmin_success() {
        RoomBan ban = RoomBan.builder()
                .id(new RoomBanId(10L, 4L)).room(publicRoom).user(outsider)
                .bannedBy(admin).bannedAt(OffsetDateTime.now()).build();

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(roomBanRepository.findByIdRoomId(10L)).thenReturn(List.of(ban));

        List<BanResponse> bans = roomService.getBans(10L, 2L);

        assertThat(bans).hasSize(1);
        assertThat(bans.get(0).getUser().getUsername()).isEqualTo("dave");
    }

    @Test
    void getBans_byNonAdmin_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 3L))
                .thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> roomService.getBans(10L, 3L))
                .isInstanceOf(ForbiddenException.class);
    }
}
