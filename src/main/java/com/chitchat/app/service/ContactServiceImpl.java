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
import com.chitchat.app.entity.enums.NotificationType;
import com.chitchat.app.exception.ConflictException;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.kafka.events.NotificationEvent;
import com.chitchat.app.kafka.producer.NotificationEventProducer;
import com.chitchat.app.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserBanRepository userBanRepository;
    private final UserRepository userRepository;
    private final EntityLoaderService entityLoader;
    private final NotificationEventProducer notificationEventProducer;

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> getFriends(Long userId) {
        return contactRepository.findFriends(userId).stream()
                .map(c -> EntityMapper.toContactResponse(c, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> getIncomingRequests(Long userId) {
        return contactRepository.findByAddresseeIdAndStatus(userId, ContactStatus.PENDING).stream()
                .map(c -> EntityMapper.toContactResponse(c, userId))
                .toList();
    }

    @Override
    public ContactResponse sendFriendRequest(Long userId, SendFriendRequestDto request) {
        User sender = entityLoader.loadActiveUser(userId);
        User target = userRepository.findByUsername(request.getTargetUsername())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getTargetUsername()));

        if (sender.getId().equals(target.getId())) {
            throw new ForbiddenException("Cannot send a friend request to yourself");
        }
        if (contactRepository.findBetween(userId, target.getId()).isPresent()) {
            throw new ConflictException("A contact record already exists between these users");
        }
        if (userBanRepository.existsByBannerIdAndBannedId(target.getId(), userId)) {
            throw new ForbiddenException("This user has blocked you");
        }

        Contact contact = Contact.builder()
                .requester(sender)
                .addressee(target)
                .status(ContactStatus.PENDING)
                .message(request.getMessage())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        contactRepository.save(contact);

        notificationEventProducer.send(NotificationEvent.builder()
                .type(NotificationType.FRIEND_REQUEST)
                .targetUserId(target.getId())
                .payload("{\"fromUsername\":\"" + sender.getUsername() + "\"}")
                .timestamp(OffsetDateTime.now())
                .build());

        return EntityMapper.toContactResponse(contact, userId);
    }

    @Override
    public ContactResponse acceptFriendRequest(Long userId, Long requestId) {
        Contact contact = contactRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
        if (!contact.getAddressee().getId().equals(userId)) {
            throw new ForbiddenException("Only the addressee can accept this request");
        }
        if (contact.getStatus() == ContactStatus.ACCEPTED) {
            throw new ConflictException("Friend request already accepted");
        }
        contact.setStatus(ContactStatus.ACCEPTED);
        contact.setUpdatedAt(OffsetDateTime.now());
        contactRepository.save(contact);
        return EntityMapper.toContactResponse(contact, userId);
    }

    @Override
    public void declineFriendRequest(Long userId, Long requestId) {
        Contact contact = contactRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
        if (!contact.getAddressee().getId().equals(userId)
                && !contact.getRequester().getId().equals(userId)) {
            throw new ForbiddenException("You are not part of this friend request");
        }
        contactRepository.delete(contact);
    }

    @Override
    public void removeFriend(Long userId, Long friendUserId) {
        Contact contact = contactRepository.findAcceptedBetween(userId, friendUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));
        contactRepository.delete(contact);
    }

    @Override
    public void banUser(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new ForbiddenException("Cannot ban yourself");
        }
        if (userBanRepository.existsByBannerIdAndBannedId(userId, targetUserId)) {
            return;
        }
        User banner = entityLoader.loadActiveUser(userId);
        User banned = entityLoader.loadActiveUser(targetUserId);
        userBanRepository.save(UserBan.builder()
                .banner(banner)
                .banned(banned)
                .createdAt(OffsetDateTime.now())
                .build());

        contactRepository.findBetween(userId, targetUserId)
                .ifPresent(contactRepository::delete);
    }

    @Override
    public void unbanUser(Long userId, Long targetUserId) {
        userBanRepository.findByBannerIdAndBannedId(userId, targetUserId)
                .ifPresent(userBanRepository::delete);
    }

}
