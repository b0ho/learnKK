package com.learnkk.session.service;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.session.dto.AttendanceResponse;
import com.learnkk.session.dto.AttendanceSummaryResponse;
import com.learnkk.session.entity.Attendance;
import com.learnkk.session.entity.MeetingSession;
import com.learnkk.session.repository.AttendanceRepository;
import com.learnkk.session.repository.MeetingSessionRepository;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스케줄러리스 시간창 출석(C4, U5, W2). 요청 시점 {@code now} 를 세션 시간창과 비교(ADR-005)하며 백그라운드 잡은 없다.
 * 참여자 판정은 {@link EnrollmentService} 무권한 read 포트(U5→U4)를 경유한다.
 */
@Service
public class AttendanceService {

  private final AttendanceRepository attendanceRepository;
  private final MeetingSessionRepository sessionRepository;
  private final MeetingService meetingService;
  private final EnrollmentService enrollmentService;

  public AttendanceService(
      AttendanceRepository attendanceRepository,
      MeetingSessionRepository sessionRepository,
      MeetingService meetingService,
      EnrollmentService enrollmentService) {
    this.attendanceRepository = attendanceRepository;
    this.sessionRepository = sessionRepository;
    this.meetingService = meetingService;
    this.enrollmentService = enrollmentService;
  }

  /**
   * 멘티 self check-in(US-6.3). 세션 404 → 비참여자 403 → 모임 비활성 409 → 시간창 밖 409 순으로 검증한 뒤 출석을
   * upsert 한다. 멱등: unique(session, mentee) 위반은 기존 출석을 그대로 반환한다(무해).
   */
  @Transactional
  public AttendanceResponse checkIn(Principal principal, Long sessionId) {
    MeetingSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCodes.SESSION_NOT_FOUND, "세션을 찾을 수 없습니다."));

    Long menteeId = principal.userId();
    if (!enrollmentService.isActiveParticipant(session.getMeetingId(), menteeId)) {
      throw new ForbiddenException(ErrorCodes.ATTENDANCE_NOT_PARTICIPANT, "모임 참여자만 출석할 수 있습니다.");
    }

    MeetingResponse meeting = meetingService.getMeeting(session.getMeetingId());
    if (meeting.status() != MeetingStatus.IN_PROGRESS) {
      throw new ConflictException(
          ErrorCodes.SESSION_MEETING_NOT_ACTIVE, "진행 중인 모임에서만 출석할 수 있습니다.");
    }

    if (!session.isWithinCheckInWindow(OffsetDateTime.now())) {
      throw new ConflictException(
          ErrorCodes.ATTENDANCE_WINDOW_CLOSED, "출석 가능 시간이 아닙니다.");
    }

    // 멱등: 이미 출석했다면 기존 기록을 그대로 반환.
    return attendanceRepository
        .findBySessionIdAndMenteeId(sessionId, menteeId)
        .map(AttendanceResponse::from)
        .orElseGet(() -> saveAttendance(sessionId, menteeId));
  }

  private AttendanceResponse saveAttendance(Long sessionId, Long menteeId) {
    try {
      Attendance saved =
          attendanceRepository.saveAndFlush(new Attendance(sessionId, menteeId));
      return AttendanceResponse.from(saved);
    } catch (DataIntegrityViolationException e) {
      // 동시 중복 check-in 이 unique 제약 경합에서 진 경우: 기존 기록을 반환(멱등).
      return attendanceRepository
          .findBySessionIdAndMenteeId(sessionId, menteeId)
          .map(AttendanceResponse::from)
          .orElseThrow(
              () -> new ConflictException(ErrorCodes.ATTENDANCE_WINDOW_CLOSED, "출석 처리에 실패했습니다."));
    }
  }

  /**
   * 본인 출석 현황(US-7.4). a=출석 세션 수, S=전체 예정 세션 수, rate=(S&gt;0)?a/S:0(리뷰 S1, 0나눗셈 회피).
   */
  @Transactional(readOnly = true)
  public AttendanceSummaryResponse getMyAttendance(Principal principal, Long meetingId) {
    int scheduled = sessionRepository.countByMeetingId(meetingId);
    java.util.List<Long> attendedSessionIds =
        attendanceRepository.findAttendedSessionIds(meetingId, principal.userId());
    return AttendanceSummaryResponse.of(attendedSessionIds.size(), scheduled, attendedSessionIds);
  }
}
