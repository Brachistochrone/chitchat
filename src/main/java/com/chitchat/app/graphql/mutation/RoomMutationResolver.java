package com.chitchat.app.graphql.mutation;

import com.chitchat.app.dto.request.CreateRoomRequest;
import com.chitchat.app.dto.request.InviteToRoomRequest;
import com.chitchat.app.dto.request.UpdateRoomRequest;
import com.chitchat.app.dto.response.RoomResponse;
import com.chitchat.app.entity.enums.RoomVisibility;
import com.chitchat.app.service.RoomService;
import com.chitchat.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomMutationResolver {

    private final RoomService roomService;

    @MutationMapping
    public RoomResponse createRoom(@Argument String name, @Argument String description,
                                    @Argument RoomVisibility visibility) {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName(name);
        request.setDescription(description);
        request.setVisibility(visibility);
        return roomService.createRoom(SecurityUtil.getCurrentUserId(), request);
    }

    @MutationMapping
    public RoomResponse updateRoom(@Argument Long id, @Argument String name,
                                    @Argument String description, @Argument RoomVisibility visibility) {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setName(name);
        request.setDescription(description);
        request.setVisibility(visibility);
        return roomService.updateRoom(id, SecurityUtil.getCurrentUserId(), request);
    }

    @MutationMapping
    public boolean deleteRoom(@Argument Long id) {
        roomService.deleteRoom(id, SecurityUtil.getCurrentUserId());
        return true;
    }

    @MutationMapping
    public boolean joinRoom(@Argument Long id) {
        roomService.joinRoom(id, SecurityUtil.getCurrentUserId());
        return true;
    }

    @MutationMapping
    public boolean leaveRoom(@Argument Long id) {
        roomService.leaveRoom(id, SecurityUtil.getCurrentUserId());
        return true;
    }

    @MutationMapping
    public boolean inviteToRoom(@Argument Long roomId, @Argument String username) {
        InviteToRoomRequest request = new InviteToRoomRequest();
        request.setUsername(username);
        roomService.inviteUser(roomId, SecurityUtil.getCurrentUserId(), request);
        return true;
    }

    @MutationMapping
    public boolean promoteAdmin(@Argument Long roomId, @Argument Long userId) {
        roomService.promoteAdmin(roomId, SecurityUtil.getCurrentUserId(), userId);
        return true;
    }

    @MutationMapping
    public boolean demoteAdmin(@Argument Long roomId, @Argument Long userId) {
        roomService.demoteAdmin(roomId, SecurityUtil.getCurrentUserId(), userId);
        return true;
    }

    @MutationMapping
    public boolean kickMember(@Argument Long roomId, @Argument Long userId) {
        roomService.kickMember(roomId, SecurityUtil.getCurrentUserId(), userId);
        return true;
    }

    @MutationMapping
    public boolean banFromRoom(@Argument Long roomId, @Argument Long userId) {
        roomService.banMember(roomId, SecurityUtil.getCurrentUserId(), userId);
        return true;
    }

    @MutationMapping
    public boolean unbanFromRoom(@Argument Long roomId, @Argument Long userId) {
        roomService.unbanMember(roomId, SecurityUtil.getCurrentUserId(), userId);
        return true;
    }
}
