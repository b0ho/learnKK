package com.learnkk.session.dto;

import com.learnkk.kernel.domain.CompletionStatus;
import com.learnkk.session.entity.MenteeCompletion;
import java.time.OffsetDateTime;

/** 멘티 수료 판정 결과 뷰(판정 근거 스냅샷 포함). Entity 를 노출하지 않는 응답 DTO(NFR8). */
public record MenteeCompletionResponse(
    Long meetingId,
    Long menteeId,
    CompletionStatus status,
    int attendedCount,
    int totalScheduled,
    OffsetDateTime approvedAt) {

  public static MenteeCompletionResponse from(MenteeCompletion mc) {
    return new MenteeCompletionResponse(
        mc.getMeetingId(),
        mc.getMenteeId(),
        mc.getStatus(),
        mc.getAttendedCount(),
        mc.getTotalScheduled(),
        mc.getApprovedAt());
  }
}
