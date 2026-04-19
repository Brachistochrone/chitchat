package com.chitchat.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteToRoomRequest {

    @NotBlank
    private String username;
}
