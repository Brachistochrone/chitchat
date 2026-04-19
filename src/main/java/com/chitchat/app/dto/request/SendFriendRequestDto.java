package com.chitchat.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendFriendRequestDto {

    @NotBlank
    private String targetUsername;

    @Size(max = 255)
    private String message;
}
