package com.chitchat.app.kafka.events;

import com.chitchat.app.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private NotificationType type;
    private Long targetUserId;
    private String payload;
    private OffsetDateTime timestamp;
}
