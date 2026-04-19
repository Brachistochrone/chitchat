package com.chitchat.app.service;

import com.chitchat.app.dto.response.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {

    AttachmentResponse upload(Long uploaderId, MultipartFile file, String comment,
                              String contextType, Long contextId);

    Resource download(Long attachmentId, Long requesterId);
}
