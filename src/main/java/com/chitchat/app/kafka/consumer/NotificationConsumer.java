package com.chitchat.app.kafka.consumer;

import com.chitchat.app.graphql.subscription.SubscriptionPublisher;
import com.chitchat.app.kafka.events.NotificationEvent;
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
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final SubscriptionPublisher subscriptionPublisher;

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_NOTIFICATIONS, groupId = AppConstants.KAFKA_GROUP_ID)
    public void consume(String payload) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            subscriptionPublisher.publishNotification(event);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(event.getTargetUserId()),
                    AppConstants.WS_QUEUE_NOTIFICATIONS,
                    event);
        } catch (Exception ex) {
            log.error("Failed to process NotificationEvent: {}", ex.getMessage(), ex);
        }
    }
}
