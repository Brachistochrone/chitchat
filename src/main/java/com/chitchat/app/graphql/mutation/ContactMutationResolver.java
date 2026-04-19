package com.chitchat.app.graphql.mutation;

import com.chitchat.app.dto.request.SendFriendRequestDto;
import com.chitchat.app.dto.response.ContactResponse;
import com.chitchat.app.service.ContactService;
import com.chitchat.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ContactMutationResolver {

    private final ContactService contactService;

    @MutationMapping
    public ContactResponse sendFriendRequest(@Argument String username, @Argument String message) {
        SendFriendRequestDto dto = new SendFriendRequestDto();
        dto.setTargetUsername(username);
        dto.setMessage(message);
        return contactService.sendFriendRequest(SecurityUtil.getCurrentUserId(), dto);
    }

    @MutationMapping
    public ContactResponse acceptFriendRequest(@Argument Long requestId) {
        return contactService.acceptFriendRequest(SecurityUtil.getCurrentUserId(), requestId);
    }

    @MutationMapping
    public boolean declineFriendRequest(@Argument Long requestId) {
        contactService.declineFriendRequest(SecurityUtil.getCurrentUserId(), requestId);
        return true;
    }

    @MutationMapping
    public boolean removeFriend(@Argument Long userId) {
        contactService.removeFriend(SecurityUtil.getCurrentUserId(), userId);
        return true;
    }

    @MutationMapping
    public boolean banUser(@Argument Long userId) {
        contactService.banUser(SecurityUtil.getCurrentUserId(), userId);
        return true;
    }

    @MutationMapping
    public boolean unbanUser(@Argument Long userId) {
        contactService.unbanUser(SecurityUtil.getCurrentUserId(), userId);
        return true;
    }
}
