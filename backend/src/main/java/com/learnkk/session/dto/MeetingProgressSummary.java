package com.learnkk.session.dto;

/**
 * 모임 단위 진행 현황 집계(U5 소유 데이터 조합). 관리자 운영 현황 모니터링(U9)이 read 하는 포트 응답이다.
 *
 * <ul>
 *   <li>{@code scheduledSessions} — 전체 예정 세션 수 S
 *   <li>{@code participantCount} — 활성(APPLIED) 참여 멘티 수
 *   <li>{@code attendanceRatePercent} — 세션 기준 출석율(%) = round(총 출석 / (S × 참여자)), 분모 0 이면 0
 *   <li>{@code completionCandidates} — 수료 후보(COMPLETION_CANDIDATE) 수
 *   <li>{@code completedCount} — 수료 확정(COMPLETED) 수
 * </ul>
 */
public record MeetingProgressSummary(
    int scheduledSessions,
    int participantCount,
    int attendanceRatePercent,
    int completionCandidates,
    int completedCount) {}
