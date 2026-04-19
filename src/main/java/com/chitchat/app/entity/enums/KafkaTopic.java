package com.chitchat.app.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KafkaTopic {

    CHAT_MESSAGES("chat.messages"),
    PRESENCE_EVENTS("presence.events"),
    PRESENCE_STATE("presence.state"),
    NOTIFICATIONS("notifications");

    private final String topicName;
}
