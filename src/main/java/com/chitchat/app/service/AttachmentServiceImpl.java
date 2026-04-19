package com.chitchat.app.service;

import com.chitchat.app.dao.AttachmentRepository;
import com.chitchat.app.dao.RoomMemberRepository;
import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dto.response.AttachmentResponse;
import com.chitchat.app.entity.Attachment;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.enums.ChatType;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.exception.ValidationException;
import com.chitchat.app.util.EntityMapper;
import com.chitchat.app.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_IMAGE_SIZE = 3L * 1024 * 1024;
    private static final long MAX_FILE_SIZE  = 20L * 1024 * 1024;

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;

    @Value("${app.storage.location}")
    private String storageLocation;

    @Override
    public AttachmentResponse upload(Long uploaderId, MultipartFile file,
                                     String comment, String contextType, Long contextId) {
        String mimeType = file.getContentType();
        long fileSize = file.getSize();
        boolean isImage = FileUtil.isImage(mimeType);

        if (isImage && fileSize > MAX_IMAGE_SIZE) {
            throw new ValidationException("Image files must not exceed 3 MB");
        }
        if (!isImage && fileSize > MAX_FILE_SIZE) {
            throw new ValidationException("Files must not exceed 20 MB");
        }

        User uploader = userRepository.findById(uploaderId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extractExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;
        Path storagePath = Paths.get(storageLocation);
        Path targetPath = storagePath.resolve(storedFilename);

        try {
            Files.createDirectories(storagePath);
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException ex) {
            log.error("Failed to store file: {}", ex.getMessage(), ex);
            throw new RuntimeException("Could not store file: " + originalFilename);
        }

        Attachment attachment = Attachment.builder()
                .uploader(uploader)
                .originalFilename(originalFilename != null ? originalFilename : storedFilename)
                .storedPath(targetPath.toString())
                .fileSize(fileSize)
                .mimeType(mimeType)
                .comment(comment)
                .createdAt(OffsetDateTime.now())
                .build();
        attachmentRepository.save(attachment);

        return EntityMapper.toAttachmentResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(Long attachmentId, Long requesterId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        checkDownloadAccess(attachment, requesterId);

        try {
            Path filePath = Paths.get(attachment.getStoredPath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found on disk");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + ex.getMessage());
        }
    }

    private void checkDownloadAccess(Attachment attachment, Long requesterId) {
        if (attachment.getMessage() == null) {
            if (!attachment.getUploader().getId().equals(requesterId)) {
                throw new ForbiddenException("Access denied");
            }
            return;
        }
        if (ChatType.ROOM.equals(attachment.getMessage().getChatType())
                && attachment.getMessage().getRoom() != null) {
            Long roomId = attachment.getMessage().getRoom().getId();
            if (!roomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, requesterId)) {
                throw new ForbiddenException("You must be a room member to download this file");
            }
        } else {
            Long senderId    = attachment.getMessage().getSender().getId();
            Long recipientId = attachment.getMessage().getRecipient() != null
                    ? attachment.getMessage().getRecipient().getId() : null;
            boolean isParticipant = requesterId.equals(senderId)
                    || (recipientId != null && requesterId.equals(recipientId));
            if (!isParticipant) {
                throw new ForbiddenException("Access denied");
            }
        }
    }

}
