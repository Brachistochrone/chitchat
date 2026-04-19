package com.chitchat.app.kafka.consumer;

import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.kafka.events.PresenceStateEvent;
import com.chitchat.app.util.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceStateConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomMemberRepository roomMemberRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_PRESENCE_STATE, groupId = AppConstants.KAFKA_GROUP_ID)
    public void consume(String payload) {
        try {
            PresenceStateEvent event = objectMapper.readValue(payload, PresenceStateEvent.class);
            roomMemberRepository.findByIdUserId(event.getUserId())
                    .forEach(member -> {
                        Long roomId = member.getId().getRoomId();
                        String destination = AppConstants.WS_TOPIC_ROOMS + roomId
                                + AppConstants.WS_TOPIC_ROOMS_PRESENCE_SUFFIX;
                        messagingTemplate.convertAndSend(destination, (Object) event);
                    });
        } catch (Exception ex) {
            log.error("Failed to process PresenceStateEvent: {}", ex.getMessage(), ex);
        }
    }
}
