package com.learnkk.session.dto;

/**
 * 멘티 출석 현황: 출석 세션 수 a, 전체 예정 세션 수 S, 출석율(S&gt;0 이면 a/S, 아니면 0 — S=0 0나눗셈 회피, 리뷰 S1).
 */
public record AttendanceSummaryResponse(int attended, int totalScheduled, double rate) {

  public static AttendanceSummaryResponse of(int attended, int totalScheduled) {
    double rate = totalScheduled > 0 ? (double) attended / totalScheduled : 0.0;
    return new AttendanceSummaryResponse(attended, totalScheduled, rate);
  }
}
