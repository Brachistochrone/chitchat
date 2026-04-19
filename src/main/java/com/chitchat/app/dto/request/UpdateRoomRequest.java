package com.chitchat.app.dto.request;

import com.chitchat.app.entity.enums.RoomVisibility;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoomRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 2000)
    private String description;

    private RoomVisibility visibility;
}
