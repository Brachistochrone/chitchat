package com.chitchat.app.rest;

import com.chitchat.app.dto.request.EditMessageRequest;
import com.chitchat.app.dto.request.SendMessageRequest;
import com.chitchat.app.dto.response.MessageResponse;
import com.chitchat.app.service.MessageService;
import com.chitchat.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "Messages", description = "Messaging endpoints")
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "Send a room message")
    @PostMapping("/api/rooms/{roomId}/messages")
    public ResponseEntity<MessageResponse> sendRoomMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = messageService.sendRoomMessage(
                roomId, SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get room message history")
    @GetMapping("/api/rooms/{roomId}/messages")
    public ResponseEntity<List<MessageResponse>> getRoomMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        OffsetDateTime cursor = before != null ? OffsetDateTime.parse(before) : null;
        return ResponseEntity.ok(
                messageService.getRoomMessages(roomId, SecurityUtil.getCurrentUserId(), cursor, limit));
    }

    @Operation(summary = "Edit a message")
    @PutMapping("/api/messages/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(
                messageService.editMessage(messageId, SecurityUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "Delete a message")
    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId, SecurityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get personal chat history")
    @GetMapping("/api/chats/{userId}/messages")
    public ResponseEntity<List<MessageResponse>> getPersonalMessages(
            @PathVariable Long userId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        OffsetDateTime cursor = before != null ? OffsetDateTime.parse(before) : null;
        return ResponseEntity.ok(
                messageService.getPersonalMessages(userId, SecurityUtil.getCurrentUserId(), cursor, limit));
    }
}
