package com.learnkk.admin.dto;

/**
 * ④ 멘티 수료 대기 큐 항목(U9, US-9.1). 수료 확정 액션은 소유 Unit(U5) CompletionService 가 수행한다. 판정 근거
 * 스냅샷(a/S)을 표시용으로 포함한다.
 */
public record MenteeCompletionQueueItem(
    Long meetingId, String meetingTitle, Long menteeId, int attendedCount, int totalScheduled) {}
