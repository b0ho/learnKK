package com.learnkk.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

  @Mock private MeetingSessionRepository sessionRepository;
  @Mock private MeetingService meetingService;
  @InjectMocks private SessionService sessionService;

  private final Principal owningMentor = new Principal(1L, Role.MENTOR);
  private final Principal otherMentor = new Principal(2L, Role.MENTOR);

  private MeetingResponse meeting(MeetingStatus status) {
    return new MeetingResponse(
        10L, 1L, "t", "topic", 8, null, null, 5, "online", "c", status, null);
  }

  @Test
  void addSession_owningMentorInProgress_creates() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(sessionRepository.save(any(MeetingSession.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SessionResponse res =
        sessionService.addSession(
            owningMentor,
            10L,
            new CreateSessionRequest(1, OffsetDateTime.parse("2026-01-01T10:00Z"), null));

    assertThat(res.meetingId()).isEqualTo(10L);
    assertThat(res.week()).isEqualTo(1);
    // 미지정 시 기본 시간창 120분.
    assertThat(res.checkInWindowMinutes()).isEqualTo(120);
  }

  @Test
  void addSession_nonOwner_forbidden403() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));

    assertThatThrownBy(
            () ->
                sessionService.addSession(
                    otherMentor,
                    10L,
                    new CreateSessionRequest(1, OffsetDateTime.parse("2026-01-01T10:00Z"), 90)))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.SESSION_FORBIDDEN);
  }

  @Test
  void addSession_notInProgress_conflict409() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING));

    assertThatThrownBy(
            () ->
                sessionService.addSession(
                    owningMentor,
                    10L,
                    new CreateSessionRequest(1, OffsetDateTime.parse("2026-01-01T10:00Z"), null)))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.SESSION_MEETING_NOT_ACTIVE);
  }

  @Test
  void updateSession_owningMentor_reschedules() {
    MeetingSession s = new MeetingSession(10L, 1, OffsetDateTime.parse("2026-01-01T10:00Z"), 120);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(s));
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(sessionRepository.save(any(MeetingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    OffsetDateTime newAt = OffsetDateTime.parse("2026-01-02T14:00Z");
    SessionResponse res =
        sessionService.updateSession(owningMentor, 5L, new UpdateSessionRequest(newAt));

    assertThat(res.scheduledAt()).isEqualTo(newAt);
  }

  @Test
  void updateSession_missing_notFound404() {
    when(sessionRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                sessionService.updateSession(
                    owningMentor,
                    5L,
                    new UpdateSessionRequest(OffsetDateTime.parse("2026-01-02T14:00Z"))))
        .isInstanceOf(NotFoundException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.SESSION_NOT_FOUND);
  }

  @Test
  void listSessions_returnsMapped() {
    when(sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(10L))
        .thenReturn(
            List.of(new MeetingSession(10L, 1, OffsetDateTime.parse("2026-01-01T10:00Z"), 120)));

    assertThat(sessionService.listSessions(10L)).hasSize(1);
  }

  @Test
  void allScheduledSessionsEnded_allPast_true() {
    MeetingSession past = new MeetingSession(10L, 1, OffsetDateTime.now().minusDays(2), 120);
    when(sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(10L))
        .thenReturn(List.of(past));

    assertThat(sessionService.allScheduledSessionsEnded(10L)).isTrue();
  }

  @Test
  void allScheduledSessionsEnded_oneFuture_false() {
    MeetingSession past = new MeetingSession(10L, 1, OffsetDateTime.now().minusDays(2), 120);
    MeetingSession future = new MeetingSession(10L, 2, OffsetDateTime.now().plusDays(1), 120);
    when(sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(10L))
        .thenReturn(List.of(past, future));

    assertThat(sessionService.allScheduledSessionsEnded(10L)).isFalse();
  }

  @Test
  void allScheduledSessionsEnded_noSessions_vacuousTrue() {
    when(sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(10L))
        .thenReturn(List.of());

    assertThat(sessionService.allScheduledSessionsEnded(10L)).isTrue();
  }

  // --- FR-7 세션 삭제 ---

  @Test
  void deleteSession_owningMentor_deletes() {
    MeetingSession s = new MeetingSession(10L, 1, OffsetDateTime.parse("2026-01-01T10:00Z"), 120);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(s));
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));

    sessionService.deleteSession(owningMentor, 5L);

    verify(sessionRepository).delete(s);
  }

  @Test
  void deleteSession_nonOwner_forbidden403() {
    MeetingSession s = new MeetingSession(10L, 1, OffsetDateTime.parse("2026-01-01T10:00Z"), 120);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(s));
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));

    assertThatThrownBy(() -> sessionService.deleteSession(otherMentor, 5L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.SESSION_FORBIDDEN);
  }

  // --- FR-8 세션 완료 처리 ---

  @Test
  void completeSession_owningMentor_marksCompleted() {
    MeetingSession s = new MeetingSession(10L, 1, OffsetDateTime.parse("2026-01-01T10:00Z"), 120);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(s));
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(sessionRepository.save(any(MeetingSession.class))).thenAnswer(inv -> inv.getArgument(0));

    SessionResponse res = sessionService.completeSession(owningMentor, 5L);

    assertThat(res.completed()).isTrue();
  }

  @Test
  void allScheduledSessionsEnded_futureButCompleted_true() {
    // 미래 시각이지만 수동 완료된 세션은 종료로 간주(FR-8).
    MeetingSession future = new MeetingSession(10L, 1, OffsetDateTime.now().plusDays(1), 120);
    future.markCompleted();
    when(sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(10L))
        .thenReturn(List.of(future));

    assertThat(sessionService.allScheduledSessionsEnded(10L)).isTrue();
  }
}
