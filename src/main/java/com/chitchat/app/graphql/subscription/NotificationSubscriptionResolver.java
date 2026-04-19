package com.chitchat.app.graphql.subscription;

import com.chitchat.app.kafka.events.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class NotificationSubscriptionResolver {

    private final SubscriptionPublisher subscriptionPublisher;

    @SubscriptionMapping
    public Flux<NotificationEvent> notifications() {
        return subscriptionPublisher.notificationStream();
    }
}
