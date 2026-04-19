package com.chitchat.app.rest;

import com.chitchat.app.dto.request.InviteToRoomRequest;
import com.chitchat.app.dto.response.BanResponse;
import com.chitchat.app.dto.response.MemberResponse;
import com.chitchat.app.service.RoomService;
import com.chitchat.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Room Members", description = "Room membership and moderation endpoints")
@RestController
@RequestMapping("/api/rooms/{roomId}")
@RequiredArgsConstructor
public class RoomMemberController {

    private final RoomService roomService;

    @Operation(summary = "Get room members")
    @GetMapping("/members")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getMembers(roomId, SecurityUtil.getCurrentUserId()));
    }

    @Operation(summary = "Invite user to room")
    @PostMapping("/invites")
    public ResponseEntity<Void> inviteUser(
            @PathVariable Long roomId,
            @Valid @RequestBody InviteToRoomRequest request) {
        roomService.inviteUser(roomId, SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Promote member to admin")
    @PostMapping("/admins/{userId}")
    public ResponseEntity<Void> promoteAdmin(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        roomService.promoteAdmin(roomId, SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Demote admin to member")
    @DeleteMapping("/admins/{userId}")
    public ResponseEntity<Void> demoteAdmin(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        roomService.demoteAdmin(roomId, SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Kick member from room")
    @PostMapping("/members/{userId}/kick")
    public ResponseEntity<Void> kickMember(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        roomService.kickMember(roomId, SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get room ban list")
    @GetMapping("/bans")
    public ResponseEntity<List<BanResponse>> getBans(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getBans(roomId, SecurityUtil.getCurrentUserId()));
    }

    @Operation(summary = "Ban a member")
    @PostMapping("/bans/{userId}")
    public ResponseEntity<Void> banMember(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        roomService.banMember(roomId, SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unban a user")
    @DeleteMapping("/bans/{userId}")
    public ResponseEntity<Void> unbanMember(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        roomService.unbanMember(roomId, SecurityUtil.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }
}
