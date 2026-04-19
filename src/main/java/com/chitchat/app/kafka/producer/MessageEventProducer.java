package com.chitchat.app.kafka.producer;

import com.chitchat.app.entity.enums.KafkaTopic;
import com.chitchat.app.kafka.events.ChatMessageEvent;
import com.chitchat.app.util.KafkaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(ChatMessageEvent event) {
        String key = KafkaUtil.buildMessageKey(event);
        log.debug("Publishing ChatMessageEvent: type={}, key={}", event.getEventType(), key);
        kafkaTemplate.send(KafkaTopic.CHAT_MESSAGES.getTopicName(), key, event);
    }
}
