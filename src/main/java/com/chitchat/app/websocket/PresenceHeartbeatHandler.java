package com.chitchat.app.websocket;

import com.chitchat.app.entity.enums.PresenceAction;
import com.chitchat.app.entity.enums.PresenceStatus;
import com.chitchat.app.kafka.events.PresenceEvent;
import com.chitchat.app.kafka.producer.PresenceEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PresenceHeartbeatHandler {

    private static final long AFK_TIMEOUT_MS = 60_000;

    private final PresenceEventProducer presenceEventProducer;
    private final Map<Long, HeartbeatEntry> heartbeatMap = new ConcurrentHashMap<>();

    @MessageMapping("/presence/heartbeat")
    public void handleHeartbeat(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        heartbeatMap.compute(userId, (id, entry) -> {
            if (entry == null) {
                return new HeartbeatEntry(System.currentTimeMillis());
            }
            entry.recordHeartbeat();
            return entry;
        });
        presenceEventProducer.send(PresenceEvent.builder()
                .userId(userId)
                .username(principal.getName())
                .status(PresenceStatus.ONLINE)
                .action(PresenceAction.HEARTBEAT)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @Scheduled(fixedRate = 30_000)
    public void checkAfk() {
        long now = System.currentTimeMillis();
        heartbeatMap.forEach((userId, entry) -> {
            if (entry.tryMarkAfk(now, AFK_TIMEOUT_MS)) {
                presenceEventProducer.send(PresenceEvent.builder()
                        .userId(userId)
                        .username(String.valueOf(userId))
                        .status(PresenceStatus.AFK)
                        .action(PresenceAction.HEARTBEAT)
                        .timestamp(OffsetDateTime.now())
                        .build());
                log.debug("User {} marked AFK", userId);
            }
        });
    }

    public void removeUser(Long userId) {
        heartbeatMap.remove(userId);
    }

    public void trackUser(Long userId) {
        heartbeatMap.putIfAbsent(userId, new HeartbeatEntry(System.currentTimeMillis()));
    }

    static class HeartbeatEntry {
        private final AtomicLong lastHeartbeatMs;
        private final AtomicBoolean markedAfk;
        private final StampedLock lock = new StampedLock();

        HeartbeatEntry(long initialMs) {
            this.lastHeartbeatMs = new AtomicLong(initialMs);
            this.markedAfk = new AtomicBoolean(false);
        }

        void recordHeartbeat() {
            long stamp = lock.writeLock();
            try {
                lastHeartbeatMs.set(System.currentTimeMillis());
                markedAfk.set(false);
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        boolean tryMarkAfk(long now, long timeoutMs) {
            long stamp = lock.readLock();
            try {
                if (markedAfk.get()) return false;
                if (now - lastHeartbeatMs.get() <= timeoutMs) return false;
            } finally {
                lock.unlockRead(stamp);
            }
            return markedAfk.compareAndSet(false, true);
        }
    }
}
