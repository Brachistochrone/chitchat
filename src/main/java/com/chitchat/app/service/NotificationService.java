package com.chitchat.app.service;

import com.chitchat.app.dto.response.UnreadCountResponse;

import java.util.List;

public interface NotificationService {

    void incrementRoomUnread(Long roomId, Long senderIdToExclude);

    void incrementPersonalUnread(Long recipientId, Long senderId);

    void resetRoomUnread(Long userId, Long roomId);

    void resetPersonalUnread(Long userId, Long chatUserId);

    List<UnreadCountResponse> getUnreadCounts(Long userId);
}
