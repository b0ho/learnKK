package com.learnkk.messaging.dto;

import com.learnkk.messaging.entity.Message;
import java.time.OffsetDateTime;

/** A single message in a thread. */
public record MessageResponse(
    Long id,
    Long threadId,
    Long senderId,
    String body,
    OffsetDateTime readAt,
    OffsetDateTime createdAt) {

  public static MessageResponse from(Message m) {
    return new MessageResponse(
        m.getId(), m.getThreadId(), m.getSenderId(), m.getBody(), m.getReadAt(), m.getCreatedAt());
  }
}
