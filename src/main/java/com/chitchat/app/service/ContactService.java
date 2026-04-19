package com.chitchat.app.service;

import com.chitchat.app.dto.request.SendFriendRequestDto;
import com.chitchat.app.dto.response.ContactResponse;

import java.util.List;

public interface ContactService {

    List<ContactResponse> getFriends(Long userId);

    List<ContactResponse> getIncomingRequests(Long userId);

    ContactResponse sendFriendRequest(Long userId, SendFriendRequestDto request);

    ContactResponse acceptFriendRequest(Long userId, Long requestId);

    void declineFriendRequest(Long userId, Long requestId);

    void removeFriend(Long userId, Long friendUserId);

    void banUser(Long userId, Long targetUserId);

    void unbanUser(Long userId, Long targetUserId);
}
