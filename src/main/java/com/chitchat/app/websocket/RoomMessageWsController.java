package com.chitchat.app.websocket;

import com.chitchat.app.dto.request.SendMessageRequest;
import com.chitchat.app.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class RoomMessageWsController {

    private final MessageService messageService;

    @MessageMapping("/rooms/{roomId}/send")
    public void sendRoomMessage(@DestinationVariable Long roomId,
                                @Payload SendMessageRequest request,
                                Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        messageService.sendRoomMessage(roomId, userId, request);
    }

    @MessageMapping("/chats/{userId}/send")
    public void sendPersonalMessage(@DestinationVariable Long userId,
                                    @Payload SendMessageRequest request,
                                    Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        messageService.sendPersonalMessage(userId, senderId, request);
    }
}
