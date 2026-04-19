package com.chitchat.app.kafka.events;

import com.chitchat.app.entity.enums.ChatType;
import com.chitchat.app.entity.enums.MessageEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

    private Long messageId;
    private ChatType chatType;
    private Long roomId;
    private Long senderId;
    private Long recipientId;
    private String content;
    private Long replyToId;
    private List<Long> attachmentIds;
    private MessageEventType eventType;
    private OffsetDateTime createdAt;
}
