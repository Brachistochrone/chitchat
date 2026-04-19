package com.chitchat.app.dao;

import com.chitchat.app.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.message.room.id = :roomId")
    void deleteAllByMessageRoomId(@Param("roomId") Long roomId);

    @Query("SELECT a FROM Attachment a WHERE a.message.id IN :messageIds")
    List<Attachment> findByMessageIdIn(@Param("messageIds") List<Long> messageIds);

    @Query("SELECT a FROM Attachment a WHERE a.message.id = :messageId")
    List<Attachment> findByMessageId(@Param("messageId") Long messageId);

    @Query("SELECT a FROM Attachment a WHERE a.uploader.id = :uploaderId")
    List<Attachment> findByUploaderId(@Param("uploaderId") Long uploaderId);
}
