package com.learnkk.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.learnkk.admin.dto.MeetingMonitoringSummary;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import com.learnkk.session.entity.MeetingSession;
import com.learnkk.session.entity.MenteeCompletion;
import com.learnkk.session.repository.AttendanceRepository;
import com.learnkk.session.repository.MeetingSessionRepository;
import com.learnkk.session.repository.MenteeCompletionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/** U9 운영 모니터링 read 조합 단위 테스트(US-9.2): 출석율(세션 기준)·수료 진행 집계와 관리자 가드. */
@ExtendWith(MockitoExtension.class)
class AdminMonitoringServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingSessionRepository sessionRepository;
  @Mock private AttendanceRepository attendanceRepository;
  @Mock private MenteeCompletionRepository completionRepository;
  @Mock private EnrollmentService enrollmentService;
  @Mock private UserRepository userRepository;

  @InjectMocks private AdminMonitoringService service;

  private static final Principal ADMIN = new Principal(99L, Role.ADMIN);
  private static final Pageable PAGE = PageRequest.of(0, 20);

  private static Meeting meeting(long id) {
    Meeting m =
        new Meeting(1L, "자바 스터디", "java", 4, OffsetDateTime.now(), OffsetDateTime.now(), 6, null, null);
    ReflectionTestUtils.setField(m, "id", id);
    ReflectionTestUtils.setField(m, "status", MeetingStatus.IN_PROGRESS);
    return m;
  }

  /** 이미 종료된 세션(시간창 경과). */
  private static MeetingSession endedSession(long meetingId) {
    return new MeetingSession(meetingId, 1, OffsetDateTime.now().minusDays(1), 30);
  }

  /** 아직 시작 전 세션. */
  private static MeetingSession futureSession(long meetingId) {
    return new MeetingSession(meetingId, 2, OffsetDateTime.now().plusDays(1), 30);
  }

  @Test
  void listMeetings_composesAttendanceRateAndCompletionProgress() {
    when(meetingRepository.findAll(PAGE)).thenReturn(new PageImpl<>(List.of(meeting(10L)), PAGE, 1));
    when(sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(10L))
        .thenReturn(List.of(endedSession(10L), futureSession(10L)));
    when(enrollmentService.listActiveMenteeIds(10L)).thenReturn(List.of(2L, 3L));
    // 총 출석 3 / (세션 2 × 멘티 2) = 0.75
    when(attendanceRepository.countByMeetingId(10L)).thenReturn(3L);
    MenteeCompletion completed = new MenteeCompletion(10L, 2L);
    completed.applyJudgement(2, 2);
    completed.approve(OffsetDateTime.now());
    MenteeCompletion candidate = new MenteeCompletion(10L, 3L);
    candidate.applyJudgement(2, 2);
    when(completionRepository.findByMeetingId(10L)).thenReturn(List.of(completed, candidate));
    when(userRepository.findById(1L)).thenReturn(java.util.Optional.empty());

    PageResponse<MeetingMonitoringSummary> page = service.listMeetings(ADMIN, null, PAGE);

    assertThat(page.totalElements()).isEqualTo(1);
    MeetingMonitoringSummary row = page.content().get(0);
    assertThat(row.id()).isEqualTo(10L);
    assertThat(row.menteeCount()).isEqualTo(2);
    assertThat(row.sessionCount()).isEqualTo(2);
    assertThat(row.endedSessionCount()).isEqualTo(1);
    assertThat(row.attendanceRate()).isCloseTo(0.75, within(1e-9));
    assertThat(row.completedMenteeCount()).isEqualTo(1);
    assertThat(row.completionCandidateCount()).isEqualTo(1);
  }

  @Test
  void listMeetings_noSessionsOrMentees_rateIsZero() {
    when(meetingRepository.findAll(PAGE)).thenReturn(new PageImpl<>(List.of(meeting(11L)), PAGE, 1));
    when(sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(11L)).thenReturn(List.of());
    when(enrollmentService.listActiveMenteeIds(11L)).thenReturn(List.of());
    when(attendanceRepository.countByMeetingId(11L)).thenReturn(0L);
    when(completionRepository.findByMeetingId(11L)).thenReturn(List.of());
    when(userRepository.findById(1L)).thenReturn(java.util.Optional.empty());

    PageResponse<MeetingMonitoringSummary> page = service.listMeetings(ADMIN, null, PAGE);

    assertThat(page.content().get(0).attendanceRate()).isZero();
  }

  @Test
  void listMeetings_withStatusFilter_queriesByStatus() {
    when(meetingRepository.findByStatus(MeetingStatus.IN_PROGRESS, PAGE))
        .thenReturn(new PageImpl<>(List.of(), PAGE, 0));

    PageResponse<MeetingMonitoringSummary> page =
        service.listMeetings(ADMIN, MeetingStatus.IN_PROGRESS, PAGE);

    assertThat(page.totalElements()).isZero();
  }

  @Test
  void listMeetings_nonAdmin_throwsForbidden() {
    assertThatThrownBy(
            () -> service.listMeetings(new Principal(1L, Role.MENTOR), null, PAGE))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(
            () -> service.listMeetings(new Principal(2L, Role.MENTEE), null, PAGE))
        .isInstanceOf(ForbiddenException.class);
  }
}
