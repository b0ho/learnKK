package com.learnkk.session.dto;

import com.learnkk.session.entity.Attendance;
import java.time.OffsetDateTime;

/** 출석 뷰. Entity 를 노출하지 않는 응답 DTO(NFR8). */
public record AttendanceResponse(Long sessionId, Long menteeId, OffsetDateTime checkedInAt) {

  public static AttendanceResponse from(Attendance a) {
    return new AttendanceResponse(a.getSessionId(), a.getMenteeId(), a.getCheckedInAt());
  }
}
