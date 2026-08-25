package com.learnkk.session.dto;

import java.util.List;

/**
 * 멘티 출석 현황: 출석 세션 수 a, 전체 예정 세션 수 S, 출석율(S&gt;0 이면 a/S, 아니면 0 — S=0 0나눗셈 회피, 리뷰 S1),
 * 그리고 출석한 세션 id 목록(FR-5, 출석완료 상태 유지 표시용).
 */
public record AttendanceSummaryResponse(
    int attended, int totalScheduled, double rate, List<Long> attendedSessionIds) {

  /** attendedSessionIds 없이(빈 목록) 생성 — 하위호환. */
  public static AttendanceSummaryResponse of(int attended, int totalScheduled) {
    return of(attended, totalScheduled, List.of());
  }

  public static AttendanceSummaryResponse of(
      int attended, int totalScheduled, List<Long> attendedSessionIds) {
    double rate = totalScheduled > 0 ? (double) attended / totalScheduled : 0.0;
    return new AttendanceSummaryResponse(
        attended, totalScheduled, rate, attendedSessionIds == null ? List.of() : attendedSessionIds);
  }
}
