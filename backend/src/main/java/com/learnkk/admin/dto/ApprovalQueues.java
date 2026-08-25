package com.learnkk.admin.dto;

import java.util.List;

/**
 * 관리자 승인 큐 집계(U9, US-9.1, getApprovalQueues). 5개 큐를 타 Unit read 로 조합해 표시만 한다(액션은 소유
 * Unit). 각 큐 → 판정 조건:
 *
 * <ul>
 *   <li>{@code creation} — ① 개설 승인 대기 (U3 status=PENDING_APPROVAL)
 *   <li>{@code recruitConfirm} — 모집 확정 대기 (U3 status=RECRUITING AND 모집기간 종료)
 *   <li>{@code start} — ② 시작 승인 대기 (U3 status=READY_TO_START)
 *   <li>{@code meetingComplete} — ③ 모임 완료 대기 (U3 status=IN_PROGRESS AND U5 전 세션 종료)
 *   <li>{@code menteeComplete} — ④ 멘티 수료 대기 (U5 CompletionStatus=COMPLETION_CANDIDATE)
 * </ul>
 */
public record ApprovalQueues(
    List<MeetingQueueItem> creation,
    List<MeetingQueueItem> recruitConfirm,
    List<MeetingQueueItem> start,
    List<MeetingQueueItem> meetingComplete,
    List<MenteeCompletionQueueItem> menteeComplete) {}
