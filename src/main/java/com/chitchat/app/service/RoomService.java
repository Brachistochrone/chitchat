package com.chitchat.app.service;

import com.chitchat.app.dto.request.CreateRoomRequest;
import com.chitchat.app.dto.request.InviteToRoomRequest;
import com.chitchat.app.dto.request.UpdateRoomRequest;
import com.chitchat.app.dto.response.BanResponse;
import com.chitchat.app.dto.response.MemberResponse;
import com.chitchat.app.dto.response.RoomResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RoomService {

    Page<RoomResponse> searchPublicRooms(String query, int page, int size);

    RoomResponse createRoom(Long ownerId, CreateRoomRequest request);

    RoomResponse getRoom(Long roomId, Long requesterId);

    RoomResponse updateRoom(Long roomId, Long requesterId, UpdateRoomRequest request);

    void deleteRoom(Long roomId, Long requesterId);

    void joinRoom(Long roomId, Long userId);

    void leaveRoom(Long roomId, Long userId);

    List<MemberResponse> getMembers(Long roomId, Long requesterId);

    void inviteUser(Long roomId, Long requesterId, InviteToRoomRequest request);

    void promoteAdmin(Long roomId, Long requesterId, Long targetUserId);

    void demoteAdmin(Long roomId, Long requesterId, Long targetUserId);

    void kickMember(Long roomId, Long requesterId, Long targetUserId);

    List<BanResponse> getBans(Long roomId, Long requesterId);

    void banMember(Long roomId, Long requesterId, Long targetUserId);

    void unbanMember(Long roomId, Long requesterId, Long targetUserId);
}
