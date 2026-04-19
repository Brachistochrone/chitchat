package com.chitchat.app.graphql.subscription;

import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.kafka.events.PresenceStateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class PresenceSubscriptionResolver {

    private final SubscriptionPublisher subscriptionPublisher;
    private final RoomMemberRepository roomMemberRepository;

    @SubscriptionMapping
    public Flux<PresenceStateEvent> roomPresence(@Argument Long roomId) {
        return subscriptionPublisher.presenceStream()
                .filter(e -> roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, e.getUserId()));
    }
}
