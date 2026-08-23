package com.learnkk.content.dto;

import com.learnkk.content.entity.Post;
import java.time.OffsetDateTime;
import java.util.List;

/** A week post with its attachment metadata (US-4.1a/4.2). Body is required; attachments 0..n. */
public record PostResponse(
    Long id,
    Long meetingId,
    Long authorId,
    int week,
    String body,
    List<AttachmentResponse> attachments,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static PostResponse from(Post p, List<AttachmentResponse> attachments) {
    return new PostResponse(
        p.getId(),
        p.getMeetingId(),
        p.getAuthorId(),
        p.getWeek(),
        p.getBody(),
        attachments,
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}
