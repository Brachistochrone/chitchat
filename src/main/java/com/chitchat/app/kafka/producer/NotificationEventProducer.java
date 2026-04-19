package com.chitchat.app.kafka.producer;

import com.chitchat.app.entity.enums.KafkaTopic;
import com.chitchat.app.kafka.events.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(NotificationEvent event) {
        String key = String.valueOf(event.getTargetUserId());
        log.debug("Publishing NotificationEvent: type={}, target={}", event.getType(), key);
        kafkaTemplate.send(KafkaTopic.NOTIFICATIONS.getTopicName(), key, event);
    }
}
