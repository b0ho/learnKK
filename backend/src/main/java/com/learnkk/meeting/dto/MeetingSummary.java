package com.learnkk.meeting.dto;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.meeting.entity.Meeting;

/** Compact meeting view for list endpoints. */
public record MeetingSummary(
    Long id, String title, String topic, int weeks, int capacity, MeetingStatus status) {

  public static MeetingSummary from(Meeting m) {
    return new MeetingSummary(
        m.getId(), m.getTitle(), m.getTopic(), m.getWeeks(), m.getCapacity(), m.getStatus());
  }
}
