package com.chitchat.app.service;

import com.chitchat.app.dao.AttachmentRepository;
import com.chitchat.app.dao.ContactRepository;
import com.chitchat.app.dao.MessageRepository;
import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.dao.UserBanRepository;
import com.chitchat.app.dto.request.EditMessageRequest;
import com.chitchat.app.dto.request.SendMessageRequest;
import com.chitchat.app.dto.response.AttachmentResponse;
import com.chitchat.app.dto.response.MessageResponse;
import com.chitchat.app.entity.Message;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.enums.ChatType;
import com.chitchat.app.entity.enums.MessageEventType;
import com.chitchat.app.entity.enums.RoomRole;
import com.chitchat.app.entity.enums.RoomVisibility;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.kafka.producer.MessageEventProducer;
import com.chitchat.app.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final ContactRepository contactRepository;
    private final UserBanRepository userBanRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageEventProducer messageEventProducer;
    private final EntityLoaderService entityLoader;

    @Override
    public MessageResponse sendRoomMessage(Long roomId, Long senderId, SendMessageRequest request) {
        Room room = entityLoader.loadRoom(roomId);
        if (!roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, senderId)) {
            throw new ForbiddenException("You are not a member of this room");
        }
        User sender = entityLoader.loadActiveUser(senderId);
        Message replyTo = resolveReplyTo(request.getReplyToId());

        Message message = Message.builder()
                .chatType(ChatType.ROOM)
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .replyTo(replyTo)
                .createdAt(OffsetDateTime.now())
                .build();
        messageRepository.save(message);

        messageEventProducer.send(EntityMapper.toChatMessageEvent(message, MessageEventType.CREATED));
        return EntityMapper.toMessageResponse(message, List.of());
    }

    @Override
    public MessageResponse sendPersonalMessage(Long recipientId, Long senderId, SendMessageRequest request) {
        User recipient = entityLoader.loadActiveUser(recipientId);
        User sender = entityLoader.loadActiveUser(senderId);

        if (contactRepository.findAcceptedBetween(senderId, recipientId).isEmpty()) {
            throw new ForbiddenException("You must be friends to send personal messages");
        }
        if (userBanRepository.existsByBannerIdAndBannedId(recipientId, senderId)) {
            throw new ForbiddenException("You have been banned by this user");
        }
        if (userBanRepository.existsByBannerIdAndBannedId(senderId, recipientId)) {
            throw new ForbiddenException("You have banned this user");
        }

        Message replyTo = resolveReplyTo(request.getReplyToId());
        Message message = Message.builder()
                .chatType(ChatType.PERSONAL)
                .sender(sender)
                .recipient(recipient)
                .content(request.getContent())
                .replyTo(replyTo)
                .createdAt(OffsetDateTime.now())
                .build();
        messageRepository.save(message);

        messageEventProducer.send(EntityMapper.toChatMessageEvent(message, MessageEventType.CREATED));
        return EntityMapper.toMessageResponse(message, List.of());
    }

    @Override
    public MessageResponse editMessage(Long messageId, Long requesterId, EditMessageRequest request) {
        Message message = loadActiveMessage(messageId);
        if (!message.getSender().getId().equals(requesterId)) {
            throw new ForbiddenException("Only the author can edit this message");
        }
        message.setContent(request.getContent());
        message.setEditedAt(OffsetDateTime.now());
        messageRepository.save(message);

        messageEventProducer.send(EntityMapper.toChatMessageEvent(message, MessageEventType.EDITED));
        List<AttachmentResponse> attachments = attachmentRepository.findByMessageId(messageId).stream()
                .map(EntityMapper::toAttachmentResponse).toList();
        return EntityMapper.toMessageResponse(message, attachments);
    }

    @Override
    public void deleteMessage(Long messageId, Long requesterId) {
        Message message = loadActiveMessage(messageId);
        boolean isAuthor = message.getSender().getId().equals(requesterId);

        if (!isAuthor) {
            if (ChatType.ROOM.equals(message.getChatType()) && message.getRoom() != null) {
                boolean isAdminOrOwner = roomMemberRepository
                        .findByIdRoomIdAndIdUserId(message.getRoom().getId(), requesterId)
                        .map(m -> m.getRole() == RoomRole.ADMIN || m.getRole() == RoomRole.OWNER)
                        .orElse(false);
                if (!isAdminOrOwner) {
                    throw new ForbiddenException("Only the author or a room admin can delete this message");
                }
            } else {
                throw new ForbiddenException("Only the author can delete this message");
            }
        }

        message.setDeletedAt(OffsetDateTime.now());
        messageRepository.save(message);
        messageEventProducer.send(EntityMapper.toChatMessageEvent(message, MessageEventType.DELETED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getRoomMessages(Long roomId, Long requesterId,
                                                  OffsetDateTime before, int limit) {
        Room room = entityLoader.loadRoom(roomId);
        if (room.getVisibility() == RoomVisibility.PRIVATE
                && !roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, requesterId)) {
            throw new ForbiddenException("Access to private room denied");
        }
        List<Message> messages = messageRepository.findRoomMessages(
                roomId, before, PageRequest.of(0, limit));
        return toResponses(messages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getPersonalMessages(Long userId, Long requesterId,
                                                      OffsetDateTime before, int limit) {
        if (requesterId.equals(userId)) {
            throw new ForbiddenException("Cannot access personal messages with yourself");
        }
        List<Message> messages = messageRepository.findPersonalMessages(
                requesterId, userId, before, PageRequest.of(0, limit));
        return toResponses(messages);
    }

    // ── Private helpers ───────────────────────────────────────────────

    private Message loadActiveMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }

    private Message resolveReplyTo(Long replyToId) {
        if (replyToId == null) return null;
        return messageRepository.findById(replyToId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Reply-to message not found"));
    }

    private List<MessageResponse> toResponses(List<Message> messages) {
        if (messages.isEmpty()) return List.of();
        List<Long> ids = messages.stream().map(Message::getId).toList();
        Map<Long, List<AttachmentResponse>> attachmentMap = attachmentRepository
                .findByMessageIdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        a -> a.getMessage().getId(),
                        Collectors.mapping(EntityMapper::toAttachmentResponse, Collectors.toList())));
        return messages.stream()
                .map(m -> EntityMapper.toMessageResponse(m,
                        attachmentMap.getOrDefault(m.getId(), List.of())))
                .toList();
    }
}
