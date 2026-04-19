package com.chitchat.app.dao;

import com.chitchat.app.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Modifying
    @Query("DELETE FROM Message m WHERE m.room.id = :roomId")
    void deleteAllByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT m FROM Message m WHERE m.room.id = :roomId AND m.deletedAt IS NULL " +
           "AND (:before IS NULL OR m.createdAt < :before) ORDER BY m.createdAt DESC")
    List<Message> findRoomMessages(@Param("roomId") Long roomId,
                                   @Param("before") OffsetDateTime before,
                                   Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatType = com.chitchat.app.entity.enums.ChatType.PERSONAL " +
           "AND m.deletedAt IS NULL " +
           "AND ((m.sender.id = :userId1 AND m.recipient.id = :userId2) " +
           "  OR (m.sender.id = :userId2 AND m.recipient.id = :userId1)) " +
           "AND (:before IS NULL OR m.createdAt < :before) ORDER BY m.createdAt DESC")
    List<Message> findPersonalMessages(@Param("userId1") Long userId1,
                                       @Param("userId2") Long userId2,
                                       @Param("before") OffsetDateTime before,
                                       Pageable pageable);
}
