package com.chitchat.app.rest;

import com.chitchat.app.dto.response.UnreadCountResponse;
import com.chitchat.app.service.NotificationService;
import com.chitchat.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notifications", description = "Unread counts and notifications")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get unread counts for all chats")
    @GetMapping("/api/notifications/unread")
    public ResponseEntity<List<UnreadCountResponse>> getUnreadCounts() {
        return ResponseEntity.ok(
                notificationService.getUnreadCounts(SecurityUtil.getCurrentUserId()));
    }

    @Operation(summary = "Mark room messages as read")
    @PostMapping("/api/rooms/{roomId}/messages/read")
    public ResponseEntity<Void> markRoomRead(@PathVariable Long roomId) {
        notificationService.resetRoomUnread(SecurityUtil.getCurrentUserId(), roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark personal messages as read")
    @PostMapping("/api/chats/{userId}/messages/read")
    public ResponseEntity<Void> markPersonalRead(@PathVariable Long userId) {
        notificationService.resetPersonalUnread(SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }
}
