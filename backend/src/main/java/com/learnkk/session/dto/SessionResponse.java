package com.learnkk.session.dto;

import com.learnkk.session.entity.MeetingSession;
import java.time.OffsetDateTime;

/** 세션 뷰(주차·예정시각·출석 시간창). Entity 를 노출하지 않는 응답 DTO(NFR8). */
public record SessionResponse(
    Long id, Long meetingId, int week, OffsetDateTime scheduledAt, int checkInWindowMinutes) {

  public static SessionResponse from(MeetingSession s) {
    return new SessionResponse(
        s.getId(), s.getMeetingId(), s.getWeek(), s.getScheduledAt(), s.getCheckInWindowMinutes());
  }
}
