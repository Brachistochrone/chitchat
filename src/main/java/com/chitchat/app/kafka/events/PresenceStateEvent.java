package com.chitchat.app.kafka.events;

import com.chitchat.app.entity.enums.PresenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceStateEvent {

    private Long userId;
    private String username;
    private PresenceStatus status;
    private int tabCount;
    private long lastHeartbeatMs;
}
