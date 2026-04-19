package com.chitchat.app.kafka.consumer;

import com.chitchat.app.entity.enums.ChatType;
import com.chitchat.app.graphql.subscription.SubscriptionPublisher;
import com.chitchat.app.kafka.events.ChatMessageEvent;
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
public class MessageEventConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final SubscriptionPublisher subscriptionPublisher;

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_CHAT_MESSAGES, groupId = AppConstants.KAFKA_GROUP_ID)
    public void consume(String payload) {
        try {
            ChatMessageEvent event = objectMapper.readValue(payload, ChatMessageEvent.class);
            subscriptionPublisher.publishMessage(event);
            if (ChatType.ROOM.equals(event.getChatType())) {
                messagingTemplate.convertAndSend(
                        AppConstants.WS_TOPIC_ROOMS + event.getRoomId(), event);
            } else {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(event.getRecipientId()),
                        AppConstants.WS_QUEUE_MESSAGES,
                        event);
            }
        } catch (Exception ex) {
            log.error("Failed to process ChatMessageEvent: {}", ex.getMessage(), ex);
        }
    }
}
