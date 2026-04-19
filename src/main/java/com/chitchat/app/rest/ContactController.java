package com.chitchat.app.rest;

import com.chitchat.app.dto.request.SendFriendRequestDto;
import com.chitchat.app.dto.response.ContactResponse;
import com.chitchat.app.service.ContactService;
import com.chitchat.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Contacts", description = "Friends and user bans")
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @Operation(summary = "Get accepted friends")
    @GetMapping
    public ResponseEntity<List<ContactResponse>> getFriends() {
        return ResponseEntity.ok(contactService.getFriends(SecurityUtil.getCurrentUserId()));
    }

    @Operation(summary = "Send friend request")
    @PostMapping("/requests")
    public ResponseEntity<ContactResponse> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestDto request) {
        ContactResponse response = contactService.sendFriendRequest(
                SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get incoming friend requests")
    @GetMapping("/requests/incoming")
    public ResponseEntity<List<ContactResponse>> getIncomingRequests() {
        return ResponseEntity.ok(
                contactService.getIncomingRequests(SecurityUtil.getCurrentUserId()));
    }

    @Operation(summary = "Accept friend request")
    @PutMapping("/requests/{requestId}/accept")
    public ResponseEntity<ContactResponse> acceptFriendRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(
                contactService.acceptFriendRequest(SecurityUtil.getCurrentUserId(), requestId));
    }

    @Operation(summary = "Decline or cancel friend request")
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> declineFriendRequest(@PathVariable Long requestId) {
        contactService.declineFriendRequest(SecurityUtil.getCurrentUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove friend")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long userId) {
        contactService.removeFriend(SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ban user")
    @PostMapping("/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long userId) {
        contactService.banUser(SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unban user")
    @DeleteMapping("/{userId}/ban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long userId) {
        contactService.unbanUser(SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }
}
