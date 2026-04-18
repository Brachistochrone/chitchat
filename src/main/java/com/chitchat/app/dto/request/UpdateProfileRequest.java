package com.chitchat.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;
}
