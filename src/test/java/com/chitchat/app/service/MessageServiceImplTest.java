package com.chitchat.app.service;

import com.chitchat.app.dao.AttachmentRepository;
import com.chitchat.app.dao.ContactRepository;
import com.chitchat.app.dao.MessageRepository;
import com.chitchat.app.dao.RoomBanRepository;
import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.dao.UserBanRepository;
import com.chitchat.app.dto.request.EditMessageRequest;
import com.chitchat.app.dto.request.SendMessageRequest;
import com.chitchat.app.dto.response.MessageResponse;
import com.chitchat.app.entity.Contact;
import com.chitchat.app.entity.Message;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.RoomMember;
import com.chitchat.app.entity.RoomMemberId;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.enums.ChatType;
import com.chitchat.app.entity.enums.ContactStatus;
import com.chitchat.app.entity.enums.RoomRole;
import com.chitchat.app.entity.enums.RoomVisibility;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.kafka.producer.MessageEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class MessageServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private ContactRepository contactRepository;
    @Mock private UserBanRepository userBanRepository;
    @Mock private RoomBanRepository roomBanRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private MessageEventProducer messageEventProducer;
    @Mock private EntityLoaderService entityLoader;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User alice;
    private User bob;
    private User charlie;
    private Room publicRoom;
    private Room privateRoom;
    private Message roomMessage;
    private Message personalMessage;

    @BeforeEach
    void setUp() {
        alice   = User.builder().id(1L).username("alice").createdAt(OffsetDateTime.now()).build();
        bob     = User.builder().id(2L).username("bob").createdAt(OffsetDateTime.now()).build();
        charlie = User.builder().id(3L).username("charlie").createdAt(OffsetDateTime.now()).build();

        publicRoom = Room.builder()
                .id(10L).name("Public Room").visibility(RoomVisibility.PUBLIC)
                .owner(alice).createdAt(OffsetDateTime.now()).build();
        privateRoom = Room.builder()
                .id(11L).name("Private Room").visibility(RoomVisibility.PRIVATE)
                .owner(alice).createdAt(OffsetDateTime.now()).build();

        roomMessage = Message.builder()
                .id(100L).chatType(ChatType.ROOM).room(publicRoom)
                .sender(alice).content("Hello room").createdAt(OffsetDateTime.now()).build();

        personalMessage = Message.builder()
                .id(101L).chatType(ChatType.PERSONAL)
                .sender(alice).recipient(bob).content("Hello bob")
                .createdAt(OffsetDateTime.now()).build();
    }

    // ── sendRoomMessage ───────────────────────────────────────────────

    @Test
    void sendRoomMessage_success() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello!");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(10L, 1L)).thenReturn(true);
        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> {
            Message m = i.getArgument(0);
            m.setId(100L);
            return m;
        });

        MessageResponse response = messageService.sendRoomMessage(10L, 1L, request);

        assertThat(response.getContent()).isEqualTo("Hello!");
        assertThat(response.getChatType()).isEqualTo(ChatType.ROOM);
        verify(messageEventProducer).send(any());
    }

    @Test
    void sendRoomMessage_notMember_throwsForbidden() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello!");

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendRoomMessage(10L, 3L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sendRoomMessage_withReply_success() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Reply");
        request.setReplyToId(100L);

        Message original = Message.builder()
                .id(100L).chatType(ChatType.ROOM).room(publicRoom)
                .sender(bob).content("Original").createdAt(OffsetDateTime.now()).build();

        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(10L, 1L)).thenReturn(true);
        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(original));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> {
            Message m = i.getArgument(0);
            m.setId(101L);
            return m;
        });

        MessageResponse response = messageService.sendRoomMessage(10L, 1L, request);

        assertThat(response.getReplyTo()).isNotNull();
        assertThat(response.getReplyTo().getContent()).isEqualTo("Original");
    }

    // ── sendPersonalMessage ───────────────────────────────────────────

    @Test
    void sendPersonalMessage_success() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hi Bob!");

        Contact friendship = Contact.builder()
                .id(1L).requester(alice).addressee(bob)
                .status(ContactStatus.ACCEPTED).build();

        when(entityLoader.loadActiveUser(2L)).thenReturn(bob);
        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(contactRepository.findAcceptedBetween(1L, 2L)).thenReturn(Optional.of(friendship));
        when(userBanRepository.existsByBannerIdAndBannedId(2L, 1L)).thenReturn(false);
        when(userBanRepository.existsByBannerIdAndBannedId(1L, 2L)).thenReturn(false);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> {
            Message m = i.getArgument(0);
            m.setId(101L);
            return m;
        });

        MessageResponse response = messageService.sendPersonalMessage(2L, 1L, request);

        assertThat(response.getContent()).isEqualTo("Hi Bob!");
        assertThat(response.getChatType()).isEqualTo(ChatType.PERSONAL);
        verify(messageEventProducer).send(any());
    }

    @Test
    void sendPersonalMessage_notFriends_throwsForbidden() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hi!");

        when(entityLoader.loadActiveUser(2L)).thenReturn(bob);
        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(contactRepository.findAcceptedBetween(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendPersonalMessage(2L, 1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("friends");
    }

    @Test
    void sendPersonalMessage_senderBannedByRecipient_throwsForbidden() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hi!");

        Contact friendship = Contact.builder()
                .id(1L).requester(alice).addressee(bob)
                .status(ContactStatus.ACCEPTED).build();

        when(entityLoader.loadActiveUser(2L)).thenReturn(bob);
        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(contactRepository.findAcceptedBetween(1L, 2L)).thenReturn(Optional.of(friendship));
        when(userBanRepository.existsByBannerIdAndBannedId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> messageService.sendPersonalMessage(2L, 1L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sendPersonalMessage_recipientBannedBySender_throwsForbidden() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hi!");

        Contact friendship = Contact.builder()
                .id(1L).requester(alice).addressee(bob)
                .status(ContactStatus.ACCEPTED).build();

        when(entityLoader.loadActiveUser(2L)).thenReturn(bob);
        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(contactRepository.findAcceptedBetween(1L, 2L)).thenReturn(Optional.of(friendship));
        when(userBanRepository.existsByBannerIdAndBannedId(2L, 1L)).thenReturn(false);
        when(userBanRepository.existsByBannerIdAndBannedId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> messageService.sendPersonalMessage(2L, 1L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── editMessage ───────────────────────────────────────────────────

    @Test
    void editMessage_success_authorOnly() {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("Updated content");

        when(messageRepository.findById(100L)).thenReturn(Optional.of(roomMessage));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
        when(attachmentRepository.findByMessageId(100L)).thenReturn(List.of());

        MessageResponse response = messageService.editMessage(100L, 1L, request);

        assertThat(response.getContent()).isEqualTo("Updated content");
        assertThat(roomMessage.getEditedAt()).isNotNull();
        verify(messageEventProducer).send(any());
    }

    @Test
    void editMessage_notAuthor_throwsForbidden() {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("Hacked");

        when(messageRepository.findById(100L)).thenReturn(Optional.of(roomMessage));

        assertThatThrownBy(() -> messageService.editMessage(100L, 2L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void editMessage_deletedMessage_throwsResourceNotFound() {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("Edit");

        Message deleted = Message.builder()
                .id(100L).sender(alice).content("Old")
                .deletedAt(OffsetDateTime.now()).build();

        when(messageRepository.findById(100L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> messageService.editMessage(100L, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteMessage ─────────────────────────────────────────────────

    @Test
    void deleteMessage_byAuthor_success() {
        when(messageRepository.findById(100L)).thenReturn(Optional.of(roomMessage));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        messageService.deleteMessage(100L, 1L);

        assertThat(roomMessage.getDeletedAt()).isNotNull();
        verify(messageEventProducer).send(any());
    }

    @Test
    void deleteMessage_byRoomAdmin_success() {
        RoomMember adminMember = RoomMember.builder()
                .id(new RoomMemberId(10L, 2L)).room(publicRoom).user(bob)
                .role(RoomRole.ADMIN).joinedAt(OffsetDateTime.now()).build();

        when(messageRepository.findById(100L)).thenReturn(Optional.of(roomMessage));
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 2L))
                .thenReturn(Optional.of(adminMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        messageService.deleteMessage(100L, 2L);

        assertThat(roomMessage.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteMessage_unauthorized_throwsForbidden() {
        when(messageRepository.findById(100L)).thenReturn(Optional.of(roomMessage));
        when(roomMemberRepository.findByIdRoomIdAndIdUserId(10L, 3L))
                .thenReturn(Optional.of(RoomMember.builder()
                        .id(new RoomMemberId(10L, 3L)).room(publicRoom).user(charlie)
                        .role(RoomRole.MEMBER).joinedAt(OffsetDateTime.now()).build()));

        assertThatThrownBy(() -> messageService.deleteMessage(100L, 3L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── getRoomMessages ───────────────────────────────────────────────

    @Test
    void getRoomMessages_publicRoom_success() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(messageRepository.findRoomMessages(10L, null, PageRequest.of(0, 50)))
                .thenReturn(List.of(roomMessage));
        when(attachmentRepository.findByMessageIdIn(List.of(100L))).thenReturn(List.of());

        List<MessageResponse> messages = messageService.getRoomMessages(10L, 3L, null, 50);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Hello room");
    }

    @Test
    void getRoomMessages_privateRoom_nonMember_throwsForbidden() {
        when(entityLoader.loadRoom(11L)).thenReturn(privateRoom);
        when(roomMemberRepository.existsByIdRoomIdAndIdUserId(11L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> messageService.getRoomMessages(11L, 3L, null, 50))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── getPersonalMessages ───────────────────────────────────────────

    @Test
    void getPersonalMessages_success() {
        when(messageRepository.findPersonalMessages(1L, 2L, null, PageRequest.of(0, 50)))
                .thenReturn(List.of(personalMessage));
        when(attachmentRepository.findByMessageIdIn(List.of(101L))).thenReturn(List.of());

        List<MessageResponse> messages = messageService.getPersonalMessages(2L, 1L, null, 50);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Hello bob");
    }

    @Test
    void getPersonalMessages_notParticipant_throwsForbidden() {
        assertThatThrownBy(() -> messageService.getPersonalMessages(1L, 1L, null, 50))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getRoomMessages_bannedUser_throwsForbidden() {
        when(entityLoader.loadRoom(10L)).thenReturn(publicRoom);
        when(roomBanRepository.existsByIdRoomIdAndIdUserId(10L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> messageService.getRoomMessages(10L, 3L, null, 50))
                .isInstanceOf(ForbiddenException.class);
    }
}
