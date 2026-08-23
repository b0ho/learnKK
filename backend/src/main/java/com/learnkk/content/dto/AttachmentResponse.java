package com.learnkk.content.dto;

import com.learnkk.content.entity.PostAttachment;
import java.time.OffsetDateTime;

/** Attachment metadata (FR4.4) — never carries the binary payload, which is served via download. */
public record AttachmentResponse(
    Long id,
    Long postId,
    String fileName,
    String contentType,
    long sizeBytes,
    Long uploaderId,
    OffsetDateTime createdAt) {

  public static AttachmentResponse from(PostAttachment a) {
    return new AttachmentResponse(
        a.getId(),
        a.getPostId(),
        a.getFileName(),
        a.getContentType(),
        a.getSizeBytes(),
        a.getUploaderId(),
        a.getCreatedAt());
  }
}
