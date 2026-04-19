package com.chitchat.app.graphql.subscription;

import com.chitchat.app.kafka.events.ChatMessageEvent;
import com.chitchat.app.kafka.events.NotificationEvent;
import com.chitchat.app.kafka.events.PresenceStateEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class SubscriptionPublisher {

    private final Sinks.Many<ChatMessageEvent> messageSink =
            Sinks.many().multicast().onBackpressureBuffer();

    private final Sinks.Many<PresenceStateEvent> presenceSink =
            Sinks.many().multicast().onBackpressureBuffer();

    private final Sinks.Many<NotificationEvent> notificationSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publishMessage(ChatMessageEvent event) {
        messageSink.tryEmitNext(event);
    }

    public void publishPresence(PresenceStateEvent event) {
        presenceSink.tryEmitNext(event);
    }

    public void publishNotification(NotificationEvent event) {
        notificationSink.tryEmitNext(event);
    }

    public Flux<ChatMessageEvent> messageStream() {
        return messageSink.asFlux();
    }

    public Flux<PresenceStateEvent> presenceStream() {
        return presenceSink.asFlux();
    }

    public Flux<NotificationEvent> notificationStream() {
        return notificationSink.asFlux();
    }
}
