package com.chitchat.app.websocket;

import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.entity.enums.PresenceAction;
import com.chitchat.app.entity.enums.PresenceStatus;
import com.chitchat.app.kafka.events.PresenceEvent;
import com.chitchat.app.kafka.producer.PresenceEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final PresenceEventProducer presenceEventProducer;
    private final UserRepository userRepository;
    private final PresenceHeartbeatHandler heartbeatHandler;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;
        publishPresence(principal.getName(), PresenceStatus.ONLINE, PresenceAction.CONNECT);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;
        publishPresence(principal.getName(), PresenceStatus.OFFLINE, PresenceAction.DISCONNECT);
    }

    private void publishPresence(String userIdStr, PresenceStatus status, PresenceAction action) {
        try {
            Long userId = Long.parseLong(userIdStr);
            String username = userRepository.findById(userId)
                    .map(u -> u.getUsername())
                    .orElse(userIdStr);

            if (action == PresenceAction.CONNECT) {
                heartbeatHandler.trackUser(userId);
            } else if (action == PresenceAction.DISCONNECT) {
                heartbeatHandler.removeUser(userId);
            }

            presenceEventProducer.send(PresenceEvent.builder()
                    .userId(userId)
                    .username(username)
                    .status(status)
                    .action(action)
                    .timestamp(OffsetDateTime.now())
                    .build());
        } catch (NumberFormatException ex) {
            log.warn("Cannot parse userId from principal: {}", userIdStr);
        }
    }
}
