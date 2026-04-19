package com.chitchat.app.service;

import com.chitchat.app.dao.ContactRepository;
import com.chitchat.app.dao.UserBanRepository;
import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dto.request.SendFriendRequestDto;
import com.chitchat.app.dto.response.ContactResponse;
import com.chitchat.app.entity.Contact;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.UserBan;
import com.chitchat.app.entity.enums.ContactStatus;
import com.chitchat.app.exception.ConflictException;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock private ContactRepository contactRepository;
    @Mock private UserBanRepository userBanRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityLoaderService entityLoader;
    @Mock private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private ContactServiceImpl contactService;

    private User alice;
    private User bob;
    private Contact pendingContact;
    private Contact acceptedContact;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).username("alice").createdAt(OffsetDateTime.now()).build();
        bob   = User.builder().id(2L).username("bob").createdAt(OffsetDateTime.now()).build();

        pendingContact = Contact.builder()
                .id(10L).requester(alice).addressee(bob)
                .status(ContactStatus.PENDING).message("Hi!")
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();

        acceptedContact = Contact.builder()
                .id(11L).requester(alice).addressee(bob)
                .status(ContactStatus.ACCEPTED)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    @Test
    void sendFriendRequest_success() {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setTargetUsername("bob");
        request.setMessage("Hi!");

        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(contactRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());
        when(userBanRepository.existsByBannerIdAndBannedId(2L, 1L)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(i -> {
            Contact c = i.getArgument(0);
            c.setId(10L);
            return c;
        });

        ContactResponse response = contactService.sendFriendRequest(1L, request);

        assertThat(response.getStatus()).isEqualTo(ContactStatus.PENDING);
        assertThat(response.getUser().getUsername()).isEqualTo("bob");
        verify(notificationEventProducer).send(any());
    }

    @Test
    void sendFriendRequest_toSelf_throwsForbidden() {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setTargetUsername("alice");

        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> contactService.sendFriendRequest(1L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sendFriendRequest_alreadyFriends_throwsConflict() {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setTargetUsername("bob");

        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(contactRepository.findBetween(1L, 2L)).thenReturn(Optional.of(acceptedContact));

        assertThatThrownBy(() -> contactService.sendFriendRequest(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sendFriendRequest_alreadyPending_throwsConflict() {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setTargetUsername("bob");

        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(contactRepository.findBetween(1L, 2L)).thenReturn(Optional.of(pendingContact));

        assertThatThrownBy(() -> contactService.sendFriendRequest(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sendFriendRequest_targetBannedRequester_throwsForbidden() {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setTargetUsername("bob");

        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(contactRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());
        when(userBanRepository.existsByBannerIdAndBannedId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> contactService.sendFriendRequest(1L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptFriendRequest_success() {
        when(contactRepository.findById(10L)).thenReturn(Optional.of(pendingContact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

        ContactResponse response = contactService.acceptFriendRequest(2L, 10L);

        assertThat(response.getStatus()).isEqualTo(ContactStatus.ACCEPTED);
    }

    @Test
    void acceptFriendRequest_notAddressee_throwsForbidden() {
        when(contactRepository.findById(10L)).thenReturn(Optional.of(pendingContact));

        assertThatThrownBy(() -> contactService.acceptFriendRequest(1L, 10L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptFriendRequest_alreadyAccepted_throwsConflict() {
        when(contactRepository.findById(11L)).thenReturn(Optional.of(acceptedContact));

        assertThatThrownBy(() -> contactService.acceptFriendRequest(2L, 11L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void declineFriendRequest_byAddressee_success() {
        when(contactRepository.findById(10L)).thenReturn(Optional.of(pendingContact));

        contactService.declineFriendRequest(2L, 10L);

        verify(contactRepository).delete(pendingContact);
    }

    @Test
    void declineFriendRequest_byRequester_success() {
        when(contactRepository.findById(10L)).thenReturn(Optional.of(pendingContact));

        contactService.declineFriendRequest(1L, 10L);

        verify(contactRepository).delete(pendingContact);
    }

    @Test
    void declineFriendRequest_byThirdParty_throwsForbidden() {
        when(contactRepository.findById(10L)).thenReturn(Optional.of(pendingContact));

        assertThatThrownBy(() -> contactService.declineFriendRequest(3L, 10L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeFriend_success() {
        when(contactRepository.findAcceptedBetween(1L, 2L)).thenReturn(Optional.of(acceptedContact));

        contactService.removeFriend(1L, 2L);

        verify(contactRepository).delete(acceptedContact);
    }

    @Test
    void removeFriend_notFriends_throwsResourceNotFound() {
        when(contactRepository.findAcceptedBetween(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.removeFriend(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void banUser_success_removesFriendship() {
        when(userBanRepository.existsByBannerIdAndBannedId(1L, 2L)).thenReturn(false);
        when(entityLoader.loadActiveUser(1L)).thenReturn(alice);
        when(entityLoader.loadActiveUser(2L)).thenReturn(bob);
        when(userBanRepository.save(any(UserBan.class))).thenAnswer(i -> i.getArgument(0));
        when(contactRepository.findBetween(1L, 2L)).thenReturn(Optional.of(acceptedContact));

        contactService.banUser(1L, 2L);

        verify(userBanRepository).save(any(UserBan.class));
        verify(contactRepository).delete(acceptedContact);
    }

    @Test
    void banUser_alreadyBanned_idempotent() {
        when(userBanRepository.existsByBannerIdAndBannedId(1L, 2L)).thenReturn(true);

        contactService.banUser(1L, 2L);

        verify(userBanRepository, never()).save(any());
    }

    @Test
    void unbanUser_success() {
        UserBan ban = UserBan.builder().id(1L).banner(alice).banned(bob).build();
        when(userBanRepository.findByBannerIdAndBannedId(1L, 2L)).thenReturn(Optional.of(ban));

        contactService.unbanUser(1L, 2L);

        verify(userBanRepository).delete(ban);
    }

    @Test
    void getFriends_returnsBothDirections() {
        Contact c1 = Contact.builder().id(10L).requester(alice).addressee(bob)
                .status(ContactStatus.ACCEPTED).createdAt(OffsetDateTime.now()).build();

        when(contactRepository.findFriends(1L)).thenReturn(List.of(c1));

        List<ContactResponse> friends = contactService.getFriends(1L);

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getUser().getUsername()).isEqualTo("bob");
    }

    @Test
    void getIncomingRequests_returnsPendingOnly() {
        when(contactRepository.findByAddresseeIdAndStatus(2L, ContactStatus.PENDING))
                .thenReturn(List.of(pendingContact));

        List<ContactResponse> requests = contactService.getIncomingRequests(2L);

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getStatus()).isEqualTo(ContactStatus.PENDING);
    }
}
