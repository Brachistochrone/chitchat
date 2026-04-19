package com.chitchat.app.service;

import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.dao.UnreadCountRepository;
import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dto.response.UnreadCountResponse;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.RoomMember;
import com.chitchat.app.entity.RoomMemberId;
import com.chitchat.app.entity.UnreadCount;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.enums.RoomRole;
import com.chitchat.app.entity.enums.RoomVisibility;
import com.chitchat.app.kafka.producer.NotificationEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private UnreadCountRepository unreadCountRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User alice;
    private User bob;
    private User charlie;
    private Room room;

    @BeforeEach
    void setUp() {
        alice   = User.builder().id(1L).username("alice").createdAt(OffsetDateTime.now()).build();
        bob     = User.builder().id(2L).username("bob").createdAt(OffsetDateTime.now()).build();
        charlie = User.builder().id(3L).username("charlie").createdAt(OffsetDateTime.now()).build();

        room = Room.builder()
                .id(10L).name("Room").visibility(RoomVisibility.PUBLIC)
                .owner(alice).createdAt(OffsetDateTime.now()).build();
    }

    @Test
    void incrementRoomUnread_success() {
        RoomMember m1 = RoomMember.builder()
                .id(new RoomMemberId(10L, 1L)).room(room).user(alice)
                .role(RoomRole.OWNER).joinedAt(OffsetDateTime.now()).build();
        RoomMember m2 = RoomMember.builder()
                .id(new RoomMemberId(10L, 2L)).room(room).user(bob)
                .role(RoomRole.MEMBER).joinedAt(OffsetDateTime.now()).build();
        RoomMember m3 = RoomMember.builder()
                .id(new RoomMemberId(10L, 3L)).room(room).user(charlie)
                .role(RoomRole.MEMBER).joinedAt(OffsetDateTime.now()).build();

        when(roomMemberRepository.findByIdRoomId(10L)).thenReturn(List.of(m1, m2, m3));
        when(unreadCountRepository.findByUserIdAndRoomId(any(), any())).thenReturn(Optional.empty());
        when(unreadCountRepository.save(any(UnreadCount.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.incrementRoomUnread(10L, 1L);

        verify(unreadCountRepository, times(2)).save(any(UnreadCount.class));
        verify(notificationEventProducer, times(2)).send(any());
    }

    @Test
    void incrementRoomUnread_excludesSender() {
        RoomMember m1 = RoomMember.builder()
                .id(new RoomMemberId(10L, 1L)).room(room).user(alice)
                .role(RoomRole.OWNER).joinedAt(OffsetDateTime.now()).build();

        when(roomMemberRepository.findByIdRoomId(10L)).thenReturn(List.of(m1));

        notificationService.incrementRoomUnread(10L, 1L);

        verify(unreadCountRepository, never()).save(any());
    }

    @Test
    void incrementPersonalUnread_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(unreadCountRepository.findByUserIdAndChatUserId(2L, 1L)).thenReturn(Optional.empty());
        when(unreadCountRepository.save(any(UnreadCount.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.incrementPersonalUnread(2L, 1L);

        verify(unreadCountRepository).save(any(UnreadCount.class));
        verify(notificationEventProducer).send(any());
    }

    @Test
    void resetRoomUnread_success() {
        UnreadCount uc = UnreadCount.builder()
                .id(1L).user(alice).room(room).count(5).build();

        when(unreadCountRepository.findByUserIdAndRoomId(1L, 10L)).thenReturn(Optional.of(uc));
        when(unreadCountRepository.save(any(UnreadCount.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.resetRoomUnread(1L, 10L);

        assertThat(uc.getCount()).isZero();
        verify(unreadCountRepository).save(uc);
    }

    @Test
    void resetPersonalUnread_success() {
        UnreadCount uc = UnreadCount.builder()
                .id(1L).user(alice).chatUser(bob).count(3).build();

        when(unreadCountRepository.findByUserIdAndChatUserId(1L, 2L)).thenReturn(Optional.of(uc));
        when(unreadCountRepository.save(any(UnreadCount.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.resetPersonalUnread(1L, 2L);

        assertThat(uc.getCount()).isZero();
    }

    @Test
    void getUnreadCounts_returnsOnlyNonZero() {
        UnreadCount uc = UnreadCount.builder()
                .id(1L).user(alice).room(room).count(3).build();

        when(unreadCountRepository.findByUserIdAndCountGreaterThan(1L, 0))
                .thenReturn(List.of(uc));

        List<UnreadCountResponse> result = notificationService.getUnreadCounts(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomId()).isEqualTo(10L);
        assertThat(result.get(0).getCount()).isEqualTo(3);
    }
}
