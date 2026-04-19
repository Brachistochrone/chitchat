package com.chitchat.app.kafka.producer;

import com.chitchat.app.entity.enums.KafkaTopic;
import com.chitchat.app.kafka.events.PresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(PresenceEvent event) {
        String key = String.valueOf(event.getUserId());
        log.debug("Publishing PresenceEvent: userId={}, status={}", event.getUserId(), event.getStatus());
        kafkaTemplate.send(KafkaTopic.PRESENCE_EVENTS.getTopicName(), key, event);
    }
}
