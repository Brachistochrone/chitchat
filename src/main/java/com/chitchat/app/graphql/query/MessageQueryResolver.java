package com.chitchat.app.graphql.query;

import com.chitchat.app.dto.response.MessagePage;
import com.chitchat.app.dto.response.MessageResponse;
import com.chitchat.app.service.MessageService;
import com.chitchat.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MessageQueryResolver {

    private final MessageService messageService;

    @QueryMapping
    public MessagePage roomMessages(@Argument Long roomId, @Argument String before,
                                     @Argument Integer limit) {
        OffsetDateTime cursor = before != null ? OffsetDateTime.parse(before) : null;
        int lim = limit != null ? limit : 50;
        List<MessageResponse> items = messageService.getRoomMessages(
                roomId, SecurityUtil.getCurrentUserId(), cursor, lim);
        String nextCursor = items.isEmpty() ? null
                : items.get(items.size() - 1).getCreatedAt().toString();
        return MessagePage.builder().items(items).nextCursor(nextCursor).build();
    }

    @QueryMapping
    public MessagePage personalMessages(@Argument Long userId, @Argument String before,
                                         @Argument Integer limit) {
        OffsetDateTime cursor = before != null ? OffsetDateTime.parse(before) : null;
        int lim = limit != null ? limit : 50;
        List<MessageResponse> items = messageService.getPersonalMessages(
                userId, SecurityUtil.getCurrentUserId(), cursor, lim);
        String nextCursor = items.isEmpty() ? null
                : items.get(items.size() - 1).getCreatedAt().toString();
        return MessagePage.builder().items(items).nextCursor(nextCursor).build();
    }
}
