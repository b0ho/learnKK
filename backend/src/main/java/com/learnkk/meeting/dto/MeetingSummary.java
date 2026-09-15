package com.learnkk.meeting.dto;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.MentorCompletionStatus;
import com.learnkk.meeting.entity.Meeting;

/** Compact meeting view for list endpoints. */
public record MeetingSummary(
    Long id,
    String title,
    String topic,
    int weeks,
    int capacity,
    MeetingStatus status,
    MentorCompletionStatus mentorCompletionStatus,
    int enrolledCount,
    boolean full) {

  /**
   * Base projection with no enrollment count known. Used by list paths that do not surface the
   * apply/capacity UI (admin queue, mentor's own meetings) — {@code enrolledCount} defaults to 0
   * and {@code full} to false. The recruiting listing enriches counts via {@link #from(Meeting,
   * int)} at the controller layer.
   */
  public static MeetingSummary from(Meeting m) {
    return from(m, 0);
  }

  /**
   * Projection carrying the meeting's active (APPLIED) enrollment count. {@code full} is derived as
   * {@code enrolledCount >= capacity}, matching the backend capacity check (BR-U4-1).
   */
  public static MeetingSummary from(Meeting m, int enrolledCount) {
    return new MeetingSummary(
        m.getId(),
        m.getTitle(),
        m.getTopic(),
        m.getWeeks(),
        m.getCapacity(),
        m.getStatus(),
        m.getMentorCompletionStatus(),
        enrolledCount,
        enrolledCount >= m.getCapacity());
  }
}
