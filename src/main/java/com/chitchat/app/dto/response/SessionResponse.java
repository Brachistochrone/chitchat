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
public class SessionResponse {

    private Long id;
    private String browser;
    private String ipAddress;
    private OffsetDateTime lastSeenAt;
    private boolean current;
}
