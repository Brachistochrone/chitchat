package com.chitchat.app.util;

import com.chitchat.app.dto.response.AttachmentResponse;
import com.chitchat.app.dto.response.AuthResponse;
import com.chitchat.app.dto.response.BanResponse;
import com.chitchat.app.dto.response.UnreadCountResponse;
import com.chitchat.app.entity.enums.MessageEventType;
import com.chitchat.app.kafka.events.ChatMessageEvent;
import com.chitchat.app.dto.response.MemberResponse;
import com.chitchat.app.dto.response.MessageResponse;
import com.chitchat.app.dto.response.RoomResponse;
import com.chitchat.app.dto.response.SessionResponse;
import com.chitchat.app.dto.response.UserResponse;
import com.chitchat.app.entity.Attachment;
import com.chitchat.app.entity.Message;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.RoomBan;
import com.chitchat.app.entity.RoomMember;
import com.chitchat.app.entity.UnreadCount;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.UserSession;

import java.util.List;

public final class EntityMapper {

    private EntityMapper() {}

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static SessionResponse toSessionResponse(UserSession session, String currentJti) {
        return SessionResponse.builder()
                .id(session.getId())
                .browser(session.getBrowser())
                .ipAddress(session.getIpAddress())
                .lastSeenAt(session.getLastSeenAt())
                .current(session.getTokenHash().equals(currentJti))
                .build();
    }

    public static AuthResponse toAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .build();
    }

    public static RoomResponse toRoomResponse(Room room, int memberCount) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .visibility(room.getVisibility())
                .memberCount(memberCount)
                .createdAt(room.getCreatedAt())
                .owner(toUserResponse(room.getOwner()))
                .build();
    }

    public static MemberResponse toMemberResponse(RoomMember member) {
        return MemberResponse.builder()
                .user(toUserResponse(member.getUser()))
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    public static BanResponse toBanResponse(RoomBan ban) {
        return BanResponse.builder()
                .user(toUserResponse(ban.getUser()))
                .bannedBy(toUserResponse(ban.getBannedBy()))
                .bannedAt(ban.getBannedAt())
                .build();
    }

    public static AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .originalFilename(attachment.getOriginalFilename())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .comment(attachment.getComment())
                .downloadUrl("/api/attachments/" + attachment.getId())
                .build();
    }

    public static MessageResponse toMessageResponse(Message message, List<AttachmentResponse> attachments) {
        MessageResponse replyTo = null;
        if (message.getReplyTo() != null) {
            replyTo = toMessageResponse(message.getReplyTo(), List.of());
        }
        return MessageResponse.builder()
                .id(message.getId())
                .chatType(message.getChatType())
                .sender(toUserResponse(message.getSender()))
                .content(message.getContent())
                .replyTo(replyTo)
                .attachments(attachments)
                .editedAt(message.getEditedAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ChatMessageEvent toChatMessageEvent(Message message, MessageEventType eventType) {
        return ChatMessageEvent.builder()
                .messageId(message.getId())
                .chatType(message.getChatType())
                .roomId(message.getRoom() != null ? message.getRoom().getId() : null)
                .senderId(message.getSender().getId())
                .recipientId(message.getRecipient() != null ? message.getRecipient().getId() : null)
                .content(message.getContent())
                .replyToId(message.getReplyTo() != null ? message.getReplyTo().getId() : null)
                .attachmentIds(List.of())
                .eventType(eventType)
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static UnreadCountResponse toUnreadCountResponse(UnreadCount unreadCount) {
        return UnreadCountResponse.builder()
                .roomId(unreadCount.getRoom() != null ? unreadCount.getRoom().getId() : null)
                .chatUserId(unreadCount.getChatUser() != null ? unreadCount.getChatUser().getId() : null)
                .count(unreadCount.getCount())
                .build();
    }
}
