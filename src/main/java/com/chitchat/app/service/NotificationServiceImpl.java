package com.chitchat.app.service;

import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.dao.UnreadCountRepository;
import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dto.response.UnreadCountResponse;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.UnreadCount;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.enums.NotificationType;
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
public class NotificationServiceImpl implements NotificationService {

    private final UnreadCountRepository unreadCountRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final NotificationEventProducer notificationEventProducer;

    @Override
    public void incrementRoomUnread(Long roomId, Long senderIdToExclude) {
        roomMemberRepository.findByIdRoomId(roomId).forEach(member -> {
            Long memberId = member.getId().getUserId();
            if (!memberId.equals(senderIdToExclude)) {
                UnreadCount uc = unreadCountRepository.findByUserIdAndRoomId(memberId, roomId)
                        .orElseGet(() -> UnreadCount.builder()
                                .user(member.getUser())
                                .room(member.getRoom())
                                .count(0)
                                .build());
                uc.setCount(uc.getCount() + 1);
                unreadCountRepository.save(uc);

                publishUnreadUpdate(memberId);
            }
        });
    }

    @Override
    public void incrementPersonalUnread(Long recipientId, Long senderId) {
        User recipient = userRepository.findById(recipientId).orElse(null);
        User sender = userRepository.findById(senderId).orElse(null);
        if (recipient == null || sender == null) return;

        UnreadCount uc = unreadCountRepository.findByUserIdAndChatUserId(recipientId, senderId)
                .orElseGet(() -> UnreadCount.builder()
                        .user(recipient)
                        .chatUser(sender)
                        .count(0)
                        .build());
        uc.setCount(uc.getCount() + 1);
        unreadCountRepository.save(uc);

        publishUnreadUpdate(recipientId);
    }

    @Override
    public void resetRoomUnread(Long userId, Long roomId) {
        unreadCountRepository.findByUserIdAndRoomId(userId, roomId)
                .ifPresent(uc -> {
                    uc.setCount(0);
                    unreadCountRepository.save(uc);
                });
    }

    @Override
    public void resetPersonalUnread(Long userId, Long chatUserId) {
        unreadCountRepository.findByUserIdAndChatUserId(userId, chatUserId)
                .ifPresent(uc -> {
                    uc.setCount(0);
                    unreadCountRepository.save(uc);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnreadCountResponse> getUnreadCounts(Long userId) {
        return unreadCountRepository.findByUserIdAndCountGreaterThan(userId, 0).stream()
                .map(EntityMapper::toUnreadCountResponse)
                .toList();
    }

    private void publishUnreadUpdate(Long targetUserId) {
        notificationEventProducer.send(NotificationEvent.builder()
                .type(NotificationType.UNREAD_UPDATE)
                .targetUserId(targetUserId)
                .payload("{}")
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
