package com.chitchat.app.util;

import com.chitchat.app.dto.response.AuthResponse;
import com.chitchat.app.dto.response.BanResponse;
import com.chitchat.app.dto.response.MemberResponse;
import com.chitchat.app.dto.response.RoomResponse;
import com.chitchat.app.dto.response.SessionResponse;
import com.chitchat.app.dto.response.UserResponse;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.RoomBan;
import com.chitchat.app.entity.RoomMember;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.UserSession;

public final class EntityMapper {

    private EntityMapper() {}

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static SessionResponse toSessionResponse(UserSession session, String currentJti) {
        return SessionResponse.builder()
                .id(session.getId())
                .browser(session.getBrowser())
                .ipAddress(session.getIpAddress())
                .lastSeenAt(session.getLastSeenAt())
                .current(session.getTokenHash().equals(currentJti))
                .build();
    }

    public static AuthResponse toAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .build();
    }

    public static RoomResponse toRoomResponse(Room room, int memberCount) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .visibility(room.getVisibility())
                .memberCount(memberCount)
                .createdAt(room.getCreatedAt())
                .owner(toUserResponse(room.getOwner()))
                .build();
    }

    public static MemberResponse toMemberResponse(RoomMember member) {
        return MemberResponse.builder()
                .user(toUserResponse(member.getUser()))
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    public static BanResponse toBanResponse(RoomBan ban) {
        return BanResponse.builder()
                .user(toUserResponse(ban.getUser()))
                .bannedBy(toUserResponse(ban.getBannedBy()))
                .bannedAt(ban.getBannedAt())
                .build();
    }
}
