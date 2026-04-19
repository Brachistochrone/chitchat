package com.chitchat.app.rest;

import com.chitchat.app.dto.request.CreateRoomRequest;
import com.chitchat.app.dto.request.UpdateRoomRequest;
import com.chitchat.app.dto.response.RoomResponse;
import com.chitchat.app.service.RoomService;
import com.chitchat.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rooms", description = "Room management endpoints")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "Search public rooms")
    @GetMapping
    public ResponseEntity<Page<RoomResponse>> searchPublicRooms(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(roomService.searchPublicRooms(q, page, size));
    }

    @Operation(summary = "Create a new room")
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get room by ID")
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoom(roomId, SecurityUtil.getCurrentUserId()));
    }

    @Operation(summary = "Update room")
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, SecurityUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "Delete room")
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        roomService.deleteRoom(roomId, SecurityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Join a public room")
    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(@PathVariable Long roomId) {
        roomService.joinRoom(roomId, SecurityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Leave a room")
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId) {
        roomService.leaveRoom(roomId, SecurityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
