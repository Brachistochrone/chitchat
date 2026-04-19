package com.chitchat.app.rest;

import com.chitchat.app.dto.response.AttachmentResponse;
import com.chitchat.app.service.AttachmentService;
import com.chitchat.app.util.FileUtil;
import com.chitchat.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Attachments", description = "File upload and download endpoints")
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(summary = "Upload an attachment")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String contextType,
            @RequestParam(required = false) Long contextId) {
        AttachmentResponse response = attachmentService.upload(
                SecurityUtil.getCurrentUserId(), file, comment, contextType, contextId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Download an attachment")
    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        Resource resource = attachmentService.download(attachmentId, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        FileUtil.contentDisposition(resource.getFilename()))
                .body(resource);
    }
}
