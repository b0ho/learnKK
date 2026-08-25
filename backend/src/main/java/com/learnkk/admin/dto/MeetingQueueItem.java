package com.learnkk.admin.dto;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.meeting.dto.MeetingResponse;

/** 승인 큐(모임 단위) 최소 표시 항목(U9, US-9.1). 실제 승인 액션은 소유 Unit(U3) Service 가 수행한다. */
public record MeetingQueueItem(
    Long id, String title, Long mentorId, MeetingStatus status, int capacity) {

  public static MeetingQueueItem from(MeetingResponse m) {
    return new MeetingQueueItem(m.id(), m.title(), m.mentorId(), m.status(), m.capacity());
  }
}
