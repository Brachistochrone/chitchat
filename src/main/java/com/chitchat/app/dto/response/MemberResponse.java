package com.chitchat.app.dto.response;

import com.chitchat.app.entity.enums.RoomRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {

    private UserResponse user;
    private RoomRole role;
    private OffsetDateTime joinedAt;
}
