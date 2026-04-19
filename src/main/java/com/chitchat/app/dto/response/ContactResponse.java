package com.chitchat.app.dto.response;

import com.chitchat.app.entity.enums.ContactStatus;
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
public class ContactResponse {

    private Long id;
    private UserResponse user;
    private ContactStatus status;
    private String message;
    private OffsetDateTime createdAt;
}
