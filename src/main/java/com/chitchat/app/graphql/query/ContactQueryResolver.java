package com.chitchat.app.graphql.query;

import com.chitchat.app.dto.response.ContactResponse;
import com.chitchat.app.service.ContactService;
import com.chitchat.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ContactQueryResolver {

    private final ContactService contactService;

    @QueryMapping
    public List<ContactResponse> contacts() {
        return contactService.getFriends(SecurityUtil.getCurrentUserId());
    }

    @QueryMapping
    public List<ContactResponse> pendingRequests() {
        return contactService.getIncomingRequests(SecurityUtil.getCurrentUserId());
    }
}
