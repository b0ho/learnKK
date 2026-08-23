package com.learnkk.meeting.dto;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.meeting.entity.Meeting;
import java.time.OffsetDateTime;

/** Full meeting view. */
public record MeetingResponse(
    Long id,
    Long mentorId,
    String title,
    String topic,
    int weeks,
    OffsetDateTime recruitStart,
    OffsetDateTime recruitEnd,
    int capacity,
    String format,
    String initialContent,
    MeetingStatus status,
    String rejectReason) {

  public static MeetingResponse from(Meeting m) {
    return new MeetingResponse(
        m.getId(),
        m.getMentorId(),
        m.getTitle(),
        m.getTopic(),
        m.getWeeks(),
        m.getRecruitStart(),
        m.getRecruitEnd(),
        m.getCapacity(),
        m.getFormat(),
        m.getInitialContent(),
        m.getStatus(),
        m.getRejectReason());
  }
}
