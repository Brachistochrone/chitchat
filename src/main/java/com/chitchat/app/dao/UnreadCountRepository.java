package com.chitchat.app.dao;

import com.chitchat.app.entity.UnreadCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnreadCountRepository extends JpaRepository<UnreadCount, Long> {

    Optional<UnreadCount> findByUserIdAndRoomId(Long userId, Long roomId);

    Optional<UnreadCount> findByUserIdAndChatUserId(Long userId, Long chatUserId);

    List<UnreadCount> findByUserIdAndCountGreaterThan(Long userId, int count);

    void deleteByUserId(Long userId);
}
