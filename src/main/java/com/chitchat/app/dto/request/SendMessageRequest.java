package com.chitchat.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SendMessageRequest {

    @Size(max = 3072)
    private String content;

    private Long replyToId;

    private List<Long> attachmentIds;
}
