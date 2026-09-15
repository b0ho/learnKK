package com.learnkk.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

  @Mock private AttendanceRepository attendanceRepository;
  @Mock private MeetingSessionRepository sessionRepository;
  @Mock private MeetingService meetingService;
  @Mock private EnrollmentService enrollmentService;
  @InjectMocks private AttendanceService attendanceService;

  private final Principal mentee = new Principal(2L, Role.MENTEE);

  private MeetingResponse meeting(MeetingStatus status) {
    return new MeetingResponse(
        10L, 1L, "t", "topic", 8, null, null, 5, "online", "c", status, null, null);
  }

  private MeetingSession sessionNow() {
    // 지금이 시간창 안에 들도록 시작을 조금 과거로.
    return new MeetingSession(10L, 1, OffsetDateTime.now().minusMinutes(5), 120);
  }

  @Test
  void checkIn_withinWindow_creates() {
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(sessionNow()));
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(attendanceRepository.findBySessionIdAndMenteeId(5L, 2L)).thenReturn(Optional.empty());
    when(attendanceRepository.saveAndFlush(any(Attendance.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    AttendanceResponse res = attendanceService.checkIn(mentee, 5L);

    assertThat(res.sessionId()).isEqualTo(5L);
    assertThat(res.menteeId()).isEqualTo(2L);
  }

  @Test
  void checkIn_sessionMissing_notFound404() {
    when(sessionRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> attendanceService.checkIn(mentee, 5L))
        .isInstanceOf(NotFoundException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.SESSION_NOT_FOUND);
  }

  @Test
  void checkIn_notParticipant_forbidden403() {
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(sessionNow()));
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(false);

    assertThatThrownBy(() -> attendanceService.checkIn(mentee, 5L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ATTENDANCE_NOT_PARTICIPANT);
  }

  @Test
  void checkIn_meetingNotInProgress_conflict409() {
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(sessionNow()));
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.READY_TO_START));

    assertThatThrownBy(() -> attendanceService.checkIn(mentee, 5L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.SESSION_MEETING_NOT_ACTIVE);
  }

  @Test
  void checkIn_beforeWindow_conflict409() {
    MeetingSession future = new MeetingSession(10L, 1, OffsetDateTime.now().plusHours(2), 120);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(future));
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));

    assertThatThrownBy(() -> attendanceService.checkIn(mentee, 5L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ATTENDANCE_WINDOW_CLOSED);
  }

  @Test
  void checkIn_afterWindow_conflict409() {
    MeetingSession ended = new MeetingSession(10L, 1, OffsetDateTime.now().minusHours(5), 120);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(ended));
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));

    assertThatThrownBy(() -> attendanceService.checkIn(mentee, 5L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ATTENDANCE_WINDOW_CLOSED);
  }

  @Test
  void checkIn_idempotent_returnsExisting() {
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(sessionNow()));
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    Attendance existing = new Attendance(5L, 2L);
    when(attendanceRepository.findBySessionIdAndMenteeId(5L, 2L))
        .thenReturn(Optional.of(existing));

    AttendanceResponse res = attendanceService.checkIn(mentee, 5L);

    assertThat(res.sessionId()).isEqualTo(5L);
    // save 는 호출되지 않아야 함(기존 유지).
    org.mockito.Mockito.verify(attendanceRepository, org.mockito.Mockito.never())
        .saveAndFlush(any(Attendance.class));
  }

  @Test
  void getMyAttendance_computesRate() {
    // FR-5: attended 는 출석 세션 id 목록(findAttendedSessionIds)의 크기로 산출된다.
    when(sessionRepository.countByMeetingId(10L)).thenReturn(4);
    when(attendanceRepository.findAttendedSessionIds(eq(10L), eq(2L)))
        .thenReturn(java.util.List.of(1L, 2L, 3L));

    AttendanceSummaryResponse res = attendanceService.getMyAttendance(mentee, 10L);

    assertThat(res.attended()).isEqualTo(3);
    assertThat(res.totalScheduled()).isEqualTo(4);
    assertThat(res.rate()).isEqualTo(0.75);
    assertThat(res.attendedSessionIds()).containsExactly(1L, 2L, 3L);
  }

  @Test
  void getMyAttendance_noSessions_rateZero() {
    when(sessionRepository.countByMeetingId(10L)).thenReturn(0);
    when(attendanceRepository.findAttendedSessionIds(eq(10L), eq(2L)))
        .thenReturn(java.util.List.of());

    AttendanceSummaryResponse res = attendanceService.getMyAttendance(mentee, 10L);

    assertThat(res.totalScheduled()).isZero();
    assertThat(res.rate()).isZero();
    assertThat(res.attendedSessionIds()).isEmpty();
  }
}
