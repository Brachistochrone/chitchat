package com.chitchat.app.dto.response;

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
public class BanResponse {

    private UserResponse user;
    private UserResponse bannedBy;
    private OffsetDateTime bannedAt;
}
