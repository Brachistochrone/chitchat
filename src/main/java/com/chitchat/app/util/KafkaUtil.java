package com.chitchat.app.util;

import com.chitchat.app.entity.enums.ChatType;
import com.chitchat.app.kafka.events.ChatMessageEvent;

public final class KafkaUtil {

    private KafkaUtil() {}

    public static String buildMessageKey(ChatMessageEvent event) {
        if (ChatType.ROOM.equals(event.getChatType())) {
            return String.valueOf(event.getRoomId());
        }
        long id1 = Math.min(event.getSenderId(), event.getRecipientId());
        long id2 = Math.max(event.getSenderId(), event.getRecipientId());
        return "dm:" + id1 + ":" + id2;
    }
}
