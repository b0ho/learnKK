package com.learnkk.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.learnkk.admin.dto.ApprovalQueues;
import com.learnkk.admin.dto.MeetingMonitorRow;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.CompletionStatus;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.session.dto.MeetingProgressSummary;
import com.learnkk.session.dto.MenteeCompletionResponse;
import com.learnkk.session.service.CompletionService;
import com.learnkk.session.service.SessionService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminQueryServiceTest {

  @Mock private MeetingService meetingService;
  @Mock private EnrollmentService enrollmentService;
  @Mock private CompletionService completionService;
  @Mock private SessionService sessionService;

  @InjectMocks private AdminQueryService adminQueryService;

  private final Principal admin = new Principal(9L, Role.ADMIN);
  private final Principal mentor = new Principal(1L, Role.MENTOR);

  private MeetingResponse meeting(Long id, MeetingStatus status, OffsetDateTime recruitEnd) {
    return new MeetingResponse(
        id, 10L, "모임" + id, null, 4, null, recruitEnd, 6, null, null, status, null);
  }

  @Test
  void getMonitoring_nonAdmin_forbidden() {
    assertThatThrownBy(() -> adminQueryService.getMonitoring(mentor))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void getMonitoring_composesRowFromCrossModuleReads() {
    when(meetingService.listAllMeetings())
        .thenReturn(List.of(meeting(1L, MeetingStatus.IN_PROGRESS, null)));
    when(enrollmentService.countActiveApplicants(1L)).thenReturn(4);
    when(completionService.getMeetingProgress(1L))
        .thenReturn(new MeetingProgressSummary(5, 4, 75, 2, 1));

    List<MeetingMonitorRow> rows = adminQueryService.getMonitoring(admin);

    assertThat(rows).hasSize(1);
    MeetingMonitorRow row = rows.get(0);
    assertThat(row.meetingId()).isEqualTo(1L);
    assertThat(row.applicantCount()).isEqualTo(4);
    assertThat(row.capacity()).isEqualTo(6);
    assertThat(row.participantCount()).isEqualTo(4);
    assertThat(row.attendanceRatePercent()).isEqualTo(75);
    assertThat(row.completionCandidates()).isEqualTo(2);
    assertThat(row.completedCount()).isEqualTo(1);
  }

  @Test
  void getMonitoring_gracefulOnSourceReadFailure() {
    when(meetingService.listAllMeetings())
        .thenReturn(List.of(meeting(1L, MeetingStatus.IN_PROGRESS, null)));
    when(enrollmentService.countActiveApplicants(1L)).thenThrow(new RuntimeException("boom"));
    when(completionService.getMeetingProgress(1L)).thenThrow(new RuntimeException("boom"));

    List<MeetingMonitorRow> rows = adminQueryService.getMonitoring(admin);

    // Row still returned with zeroed metrics rather than failing the whole dashboard.
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).applicantCount()).isZero();
    assertThat(rows.get(0).attendanceRatePercent()).isZero();
  }

  @Test
  void getApprovalQueues_nonAdmin_forbidden() {
    assertThatThrownBy(() -> adminQueryService.getApprovalQueues(mentor))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void getApprovalQueues_aggregatesFiveQueuesWithFilters() {
    OffsetDateTime past = OffsetDateTime.now().minusDays(1);
    OffsetDateTime future = OffsetDateTime.now().plusDays(1);

    when(meetingService.listByStatus(MeetingStatus.PENDING_APPROVAL))
        .thenReturn(List.of(meeting(1L, MeetingStatus.PENDING_APPROVAL, null)));
    // Two RECRUITING: one recruit-ended (past) qualifies, one still open (future) is filtered out.
    when(meetingService.listByStatus(MeetingStatus.RECRUITING))
        .thenReturn(
            List.of(
                meeting(2L, MeetingStatus.RECRUITING, past),
                meeting(3L, MeetingStatus.RECRUITING, future)));
    when(meetingService.listByStatus(MeetingStatus.READY_TO_START))
        .thenReturn(List.of(meeting(4L, MeetingStatus.READY_TO_START, null)));
    // Two IN_PROGRESS: only the one whose sessions all ended qualifies for ③.
    when(meetingService.listByStatus(MeetingStatus.IN_PROGRESS))
        .thenReturn(
            List.of(
                meeting(5L, MeetingStatus.IN_PROGRESS, null),
                meeting(6L, MeetingStatus.IN_PROGRESS, null)));
    when(sessionService.allScheduledSessionsEnded(5L)).thenReturn(true);
    when(sessionService.allScheduledSessionsEnded(6L)).thenReturn(false);
    when(completionService.listCompletionCandidates())
        .thenReturn(
            List.of(
                new MenteeCompletionResponse(
                    5L, 100L, CompletionStatus.COMPLETION_CANDIDATE, 4, 5, null)));
    when(meetingService.getMeeting(5L)).thenReturn(meeting(5L, MeetingStatus.IN_PROGRESS, null));

    ApprovalQueues queues = adminQueryService.getApprovalQueues(admin);

    assertThat(queues.creation()).hasSize(1);
    assertThat(queues.recruitConfirm()).hasSize(1);
    assertThat(queues.recruitConfirm().get(0).id()).isEqualTo(2L);
    assertThat(queues.start()).hasSize(1);
    assertThat(queues.meetingComplete()).hasSize(1);
    assertThat(queues.meetingComplete().get(0).id()).isEqualTo(5L);
    assertThat(queues.menteeComplete()).hasSize(1);
    assertThat(queues.menteeComplete().get(0).menteeId()).isEqualTo(100L);
    assertThat(queues.menteeComplete().get(0).meetingTitle()).isEqualTo("모임5");
  }
}
