package com.learnkk.session.service;

import com.learnkk.meeting.service.SessionCompletionGate;
import org.springframework.stereotype.Component;

/**
 * Bolt 6(U5) 실제 구현: {@link SessionCompletionGate} 시임을 세션 read 기반으로 배선한다(ADR-007 R-2). Bolt 2
 * 스텁 {@code NoSessionsCompletionGate} 를 대체한다. 상태 쓰기(COMPLETED)는 U3 소유({@code
 * MeetingApprovalService.completeMeeting})로 유지되며, 이 게이트는 전제조건만 read 한다. 세션이 없으면
 * {@link SessionService#allScheduledSessionsEnded} 가 vacuous-true 를 돌려주어 완료 게이팅에 회귀가 없다.
 */
@Component
public class SessionBackedCompletionGate implements SessionCompletionGate {

  private final SessionService sessionService;

  public SessionBackedCompletionGate(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @Override
  public boolean allScheduledSessionsEnded(Long meetingId) {
    return sessionService.allScheduledSessionsEnded(meetingId);
  }
}
