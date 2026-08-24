package com.learnkk.session.service;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.session.dto.CreateSessionRequest;
import com.learnkk.session.dto.SessionResponse;
import com.learnkk.session.dto.UpdateSessionRequest;
import com.learnkk.session.entity.MeetingSession;
import com.learnkk.session.repository.MeetingSessionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 일정 관리(C4, U5, W1) + U3 완료 게이트용 read-out. 모임 상태·소유 멘토는 {@link MeetingService} 를 통해 U3
 * 에서 read 하며(ADR-007 R-1) meetings 테이블을 직접 건드리지 않는다. 스케줄러 없이(ADR-005) 시간창 판정은 요청 시점
 * {@code now} 비교로 처리한다.
 */
@Service
public class SessionService {

  /** 출석 유효 시간창 기본 길이(분). CreateSessionRequest 미지정 시 적용(BR-U5-2 [assumption]). */
  static final int DEFAULT_CHECK_IN_WINDOW_MINUTES = 120;

  private final MeetingSessionRepository sessionRepository;
  private final MeetingService meetingService;

  public SessionService(
      MeetingSessionRepository sessionRepository, MeetingService meetingService) {
    this.sessionRepository = sessionRepository;
    this.meetingService = meetingService;
  }

  /**
   * 세션 추가(US-6.2). 소유 멘토만 가능하며(403 SESSION_FORBIDDEN), 모임이 IN_PROGRESS 여야 한다(409
   * SESSION_MEETING_NOT_ACTIVE, [assumption]).
   */
  @Transactional
  public SessionResponse addSession(Principal principal, Long meetingId, CreateSessionRequest req) {
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    requireOwningMentor(principal, meeting);
    requireInProgress(meeting);

    int window =
        req.checkInWindowMinutes() != null
            ? req.checkInWindowMinutes()
            : DEFAULT_CHECK_IN_WINDOW_MINUTES;
    MeetingSession saved =
        sessionRepository.save(new MeetingSession(meetingId, req.week(), req.scheduledAt(), window));
    return SessionResponse.from(saved);
  }

  /** 세션 일정 변경(US-6.2). 소유 멘토만 예정 시각을 갱신한다(멘티 현황 반영, A6). */
  @Transactional
  public SessionResponse updateSession(
      Principal principal, Long sessionId, UpdateSessionRequest req) {
    MeetingSession session = loadSession(sessionId);
    MeetingResponse meeting = meetingService.getMeeting(session.getMeetingId());
    requireOwningMentor(principal, meeting);

    session.reschedule(req.scheduledAt());
    return SessionResponse.from(sessionRepository.save(session));
  }

  /** 세션 목록(주차·예정시각 오름차순). 멘티/멘토/관리자 현황 read. */
  @Transactional(readOnly = true)
  public List<SessionResponse> listSessions(Long meetingId) {
    return sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(meetingId).stream()
        .map(SessionResponse::from)
        .toList();
  }

  /**
   * read-out(U3 ③ 완료 전제, ADR-007 R-2): 모임의 모든 예정 세션이 종료되었는가. 세션의 시간창 종료 시각({@code
   * scheduledAt + window})이 모두 현재 시각 이전이면 true. 세션이 없으면 vacuous-true(완료 허용, 무회귀).
   */
  @Transactional(readOnly = true)
  public boolean allScheduledSessionsEnded(Long meetingId) {
    OffsetDateTime now = OffsetDateTime.now();
    return sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(meetingId).stream()
        .allMatch(s -> s.windowEnd().isBefore(now));
  }

  private void requireOwningMentor(Principal principal, MeetingResponse meeting) {
    boolean owningMentor =
        principal.isMentor() && meeting.mentorId().equals(principal.userId());
    if (!owningMentor) {
      throw new ForbiddenException(ErrorCodes.SESSION_FORBIDDEN, "세션을 관리할 권한이 없습니다.");
    }
  }

  private void requireInProgress(MeetingResponse meeting) {
    if (meeting.status() != MeetingStatus.IN_PROGRESS) {
      throw new ConflictException(
          ErrorCodes.SESSION_MEETING_NOT_ACTIVE, "진행 중인 모임에서만 세션을 관리할 수 있습니다.");
    }
  }

  MeetingSession loadSession(Long sessionId) {
    return sessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCodes.SESSION_NOT_FOUND, "세션을 찾을 수 없습니다."));
  }
}
