package com.learnkk.survey.dto;

import com.learnkk.survey.entity.Feedback;
import java.time.OffsetDateTime;

/** A stored course feedback entry (US-8.2 read, owning mentor / admin). */
public record FeedbackResponse(Long id, Long menteeId, String content, OffsetDateTime createdAt) {

  public static FeedbackResponse from(Feedback f) {
    return new FeedbackResponse(f.getId(), f.getMenteeId(), f.getContent(), f.getCreatedAt());
  }
}
