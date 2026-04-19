package com.chitchat.app.dto.response;

import com.chitchat.app.entity.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private ChatType chatType;
    private UserResponse sender;
    private String content;
    private MessageResponse replyTo;
    private List<AttachmentResponse> attachments;
    private OffsetDateTime editedAt;
    private OffsetDateTime createdAt;
}
