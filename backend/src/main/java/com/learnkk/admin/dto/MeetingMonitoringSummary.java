package com.learnkk.admin.dto;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.MentorCompletionStatus;

/**
 * 관리자 운영 현황 모니터링 행(US-9.2, U9). 모임별 상태·출석율(세션 기준)·수료 진행을 한 행으로 조합한 read 전용
 * 뷰. 출석율은 멘티별 a/S 정의(BR-U5-3)의 모임 평균 — {@code 총 출석 수 / (전체 예정 세션 수 S × 참여 멘티 수)},
 * 분모 0이면 0.0(0나눗셈 회피).
 */
public record MeetingMonitoringSummary(
    Long id,
    String title,
    MeetingStatus status,
    Long mentorId,
    String mentorNickname,
    int menteeCount,
    int sessionCount,
    int endedSessionCount,
    double attendanceRate,
    int completedMenteeCount,
    int completionCandidateCount,
    MentorCompletionStatus mentorCompletionStatus) {}
