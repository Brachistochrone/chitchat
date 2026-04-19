package com.chitchat.app.websocket;

import com.chitchat.app.dao.UserRepository;
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

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;
        publishPresence(principal.getName(), PresenceStatus.ONLINE);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;
        publishPresence(principal.getName(), PresenceStatus.OFFLINE);
    }

    private void publishPresence(String userIdStr, PresenceStatus status) {
        try {
            Long userId = Long.parseLong(userIdStr);
            String username = userRepository.findById(userId)
                    .map(u -> u.getUsername())
                    .orElse(userIdStr);
            presenceEventProducer.send(PresenceEvent.builder()
                    .userId(userId)
                    .username(username)
                    .status(status)
                    .timestamp(OffsetDateTime.now())
                    .build());
        } catch (NumberFormatException ex) {
            log.warn("Cannot parse userId from principal: {}", userIdStr);
        }
    }
}
