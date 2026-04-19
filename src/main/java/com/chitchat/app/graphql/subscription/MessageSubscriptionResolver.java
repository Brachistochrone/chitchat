package com.chitchat.app.graphql.subscription;

import com.chitchat.app.entity.enums.ChatType;
import com.chitchat.app.kafka.events.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class MessageSubscriptionResolver {

    private final SubscriptionPublisher subscriptionPublisher;

    @SubscriptionMapping
    public Flux<ChatMessageEvent> roomMessages(@Argument Long roomId) {
        return subscriptionPublisher.messageStream()
                .filter(e -> ChatType.ROOM.equals(e.getChatType())
                        && roomId.equals(e.getRoomId()));
    }

    @SubscriptionMapping
    public Flux<ChatMessageEvent> personalMessages() {
        return subscriptionPublisher.messageStream()
                .filter(e -> ChatType.PERSONAL.equals(e.getChatType()));
    }
}
