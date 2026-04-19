package com.chitchat.app.kafka.streams;

import com.chitchat.app.entity.enums.KafkaTopic;
import com.chitchat.app.entity.enums.PresenceAction;
import com.chitchat.app.entity.enums.PresenceStatus;
import com.chitchat.app.kafka.events.PresenceEvent;
import com.chitchat.app.kafka.events.PresenceStateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceStateTopology {

    private final ObjectMapper objectMapper;

    @Bean
    public KStream<String, String> presenceStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream(
                KafkaTopic.PRESENCE_EVENTS.getTopicName());

        stream.groupByKey()
                .aggregate(
                        this::initialState,
                        (userId, eventJson, stateJson) -> aggregate(userId, eventJson, stateJson),
                        Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("presence-state-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.String()))
                .toStream()
                .to(KafkaTopic.PRESENCE_STATE.getTopicName());

        return stream;
    }

    private String initialState() {
        try {
            PresenceStateEvent state = PresenceStateEvent.builder()
                    .tabCount(0)
                    .lastHeartbeatMs(0)
                    .status(PresenceStatus.OFFLINE)
                    .build();
            return objectMapper.writeValueAsString(state);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String aggregate(String userId, String eventJson, String stateJson) {
        try {
            PresenceEvent event = objectMapper.readValue(eventJson, PresenceEvent.class);
            PresenceStateEvent state = objectMapper.readValue(stateJson, PresenceStateEvent.class);

            if (event.getAction() == null) {
                return stateJson;
            }

            switch (event.getAction()) {
                case CONNECT -> {
                    state.setTabCount(state.getTabCount() + 1);
                    state.setLastHeartbeatMs(System.currentTimeMillis());
                }
                case DISCONNECT -> {
                    state.setTabCount(Math.max(0, state.getTabCount() - 1));
                }
                case HEARTBEAT -> {
                    state.setLastHeartbeatMs(System.currentTimeMillis());
                }
            }

            state.setUserId(event.getUserId());
            state.setUsername(event.getUsername());
            state.setStatus(deriveStatus(state));

            return objectMapper.writeValueAsString(state);
        } catch (Exception ex) {
            log.error("Failed to aggregate presence event: {}", ex.getMessage());
            return stateJson;
        }
    }

    private PresenceStatus deriveStatus(PresenceStateEvent state) {
        if (state.getTabCount() <= 0) {
            return PresenceStatus.OFFLINE;
        }
        long elapsed = System.currentTimeMillis() - state.getLastHeartbeatMs();
        return elapsed < 60_000 ? PresenceStatus.ONLINE : PresenceStatus.AFK;
    }
}
