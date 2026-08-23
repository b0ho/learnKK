package com.learnkk.content.dto;

import com.learnkk.content.entity.Notice;
import java.time.OffsetDateTime;

/** A meeting notice view (US-4.3). */
public record NoticeResponse(
    Long id, Long meetingId, Long authorId, String body, OffsetDateTime createdAt) {

  public static NoticeResponse from(Notice n) {
    return new NoticeResponse(
        n.getId(), n.getMeetingId(), n.getAuthorId(), n.getBody(), n.getCreatedAt());
  }
}
