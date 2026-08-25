package com.learnkk.admin.dto;

import com.learnkk.kernel.domain.MeetingStatus;

/**
 * 운영 현황 모니터링 한 행(U9, US-9.2). 소유 데이터 없이 U3(모임 기본·상태·정원)·U4(신청 수)·U5(참여자·세션 기준
 * 출석율·수료 진행) read 를 조합한다. 집계 지표(FR9.2)는 범위 밖.
 */
public record MeetingMonitorRow(
    Long meetingId,
    String title,
    Long mentorId,
    MeetingStatus status,
    int capacity,
    int applicantCount,
    int participantCount,
    int attendanceRatePercent,
    int completionCandidates,
    int completedCount) {}
