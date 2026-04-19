package com.chitchat.app.service;

import com.chitchat.app.dto.request.EditMessageRequest;
import com.chitchat.app.dto.request.SendMessageRequest;
import com.chitchat.app.dto.response.MessageResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface MessageService {

    MessageResponse sendRoomMessage(Long roomId, Long senderId, SendMessageRequest request);

    MessageResponse sendPersonalMessage(Long recipientId, Long senderId, SendMessageRequest request);

    MessageResponse editMessage(Long messageId, Long requesterId, EditMessageRequest request);

    void deleteMessage(Long messageId, Long requesterId);

    List<MessageResponse> getRoomMessages(Long roomId, Long requesterId, OffsetDateTime before, int limit);

    List<MessageResponse> getPersonalMessages(Long userId, Long requesterId, OffsetDateTime before, int limit);
}
