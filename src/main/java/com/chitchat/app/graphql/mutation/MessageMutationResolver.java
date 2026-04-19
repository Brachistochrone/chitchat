package com.chitchat.app.graphql.mutation;

import com.chitchat.app.dto.request.EditMessageRequest;
import com.chitchat.app.dto.request.SendMessageRequest;
import com.chitchat.app.dto.response.MessageResponse;
import com.chitchat.app.service.MessageService;
import com.chitchat.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MessageMutationResolver {

    private final MessageService messageService;

    @MutationMapping
    public MessageResponse sendRoomMessage(@Argument Long roomId, @Argument String content,
                                            @Argument Long replyToId, @Argument List<Long> attachmentIds) {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent(content);
        request.setReplyToId(replyToId);
        request.setAttachmentIds(attachmentIds);
        return messageService.sendRoomMessage(roomId, SecurityUtil.getCurrentUserId(), request);
    }

    @MutationMapping
    public MessageResponse sendPersonalMessage(@Argument Long userId, @Argument String content,
                                                @Argument Long replyToId, @Argument List<Long> attachmentIds) {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent(content);
        request.setReplyToId(replyToId);
        request.setAttachmentIds(attachmentIds);
        return messageService.sendPersonalMessage(userId, SecurityUtil.getCurrentUserId(), request);
    }

    @MutationMapping
    public MessageResponse editMessage(@Argument Long id, @Argument String content) {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent(content);
        return messageService.editMessage(id, SecurityUtil.getCurrentUserId(), request);
    }

    @MutationMapping
    public boolean deleteMessage(@Argument Long id) {
        messageService.deleteMessage(id, SecurityUtil.getCurrentUserId());
        return true;
    }
}
