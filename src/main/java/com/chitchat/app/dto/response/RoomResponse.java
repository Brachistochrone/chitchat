package com.chitchat.app.dto.response;

import com.chitchat.app.entity.enums.RoomVisibility;
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
public class RoomResponse {

    private Long id;
    private String name;
    private String description;
    private RoomVisibility visibility;
    private int memberCount;
    private OffsetDateTime createdAt;
    private UserResponse owner;
}
