package com.chitchat.app.dao;

import com.chitchat.app.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.message.room.id = :roomId")
    void deleteAllByMessageRoomId(@Param("roomId") Long roomId);
}
