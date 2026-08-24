package com.learnkk.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.CompletionStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.session.dto.MenteeCompletionResponse;
import com.learnkk.session.entity.MenteeCompletion;
import com.learnkk.session.repository.AttendanceRepository;
import com.learnkk.session.repository.MenteeCompletionRepository;
import com.learnkk.session.repository.MeetingSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompletionServiceTest {

  @Mock private MenteeCompletionRepository completionRepository;
  @Mock private MeetingSessionRepository sessionRepository;
  @Mock private AttendanceRepository attendanceRepository;
  @Mock private EnrollmentService enrollmentService;
  @Mock private MeetingService meetingService;
  @InjectMocks private CompletionService completionService;

  private final Principal admin = new Principal(9L, Role.ADMIN);
  private final Principal owningMentor = new Principal(1L, Role.MENTOR);
  private final Principal otherMentor = new Principal(7L, Role.MENTOR);

  private MeetingResponse meeting() {
    return new MeetingResponse(
        10L, 1L, "t", "topic", 8, null, null, 5, "online", "c",
        com.learnkk.kernel.domain.MeetingStatus.IN_PROGRESS, null);
  }

  @Test
  void computeCompletion_boundary_exactly80_isCandidate() {
    // S=5, a=4 → 4*100=400 >= 80*5=400 → 후보.
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    when(sessionRepository.countByMeetingId(10L)).thenReturn(5);
    when(enrollmentService.listActiveMenteeIds(10L)).thenReturn(List.of(2L));
    when(attendanceRepository.countAttendedSessions(10L, 2L)).thenReturn(4L);
    when(completionRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(completionRepository.save(any(MenteeCompletion.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(completionRepository.findByMeetingId(10L))
        .thenAnswer(
            inv -> {
              MenteeCompletion mc = new MenteeCompletion(10L, 2L);
              mc.applyJudgement(4, 5);
              return List.of(mc);
            });

    List<MenteeCompletionResponse> res = completionService.computeCompletion(owningMentor, 10L);

    assertThat(res).hasSize(1);
    assertThat(res.get(0).status()).isEqualTo(CompletionStatus.COMPLETION_CANDIDATE);
  }

  @Test
  void computeCompletion_below80_notCompleted() {
    // S=5, a=3 → 300 >= 400 거짓 → 미수료.
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    when(sessionRepository.countByMeetingId(10L)).thenReturn(5);
    when(enrollmentService.listActiveMenteeIds(10L)).thenReturn(List.of(2L));
    when(attendanceRepository.countAttendedSessions(10L, 2L)).thenReturn(3L);
    when(completionRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(completionRepository.save(any(MenteeCompletion.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(completionRepository.findByMeetingId(10L))
        .thenAnswer(
            inv -> {
              MenteeCompletion mc = new MenteeCompletion(10L, 2L);
              mc.applyJudgement(3, 5);
              return List.of(mc);
            });

    List<MenteeCompletionResponse> res = completionService.computeCompletion(owningMentor, 10L);

    assertThat(res.get(0).status()).isEqualTo(CompletionStatus.NOT_COMPLETED);
  }

  @Test
  void computeCompletion_noSessions_candidateWithheld() {
    // S=0 → 후보 판정 보류(미수료).
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    when(sessionRepository.countByMeetingId(10L)).thenReturn(0);
    when(enrollmentService.listActiveMenteeIds(10L)).thenReturn(List.of(2L));
    when(attendanceRepository.countAttendedSessions(10L, 2L)).thenReturn(0L);
    when(completionRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(completionRepository.save(any(MenteeCompletion.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(completionRepository.findByMeetingId(10L))
        .thenAnswer(
            inv -> {
              MenteeCompletion mc = new MenteeCompletion(10L, 2L);
              mc.applyJudgement(0, 0);
              return List.of(mc);
            });

    List<MenteeCompletionResponse> res = completionService.computeCompletion(owningMentor, 10L);

    assertThat(res.get(0).status()).isEqualTo(CompletionStatus.NOT_COMPLETED);
  }

  @Test
  void computeCompletion_nonOwnerMentor_forbidden403() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());

    assertThatThrownBy(() -> completionService.computeCompletion(otherMentor, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.COMPLETION_FORBIDDEN);
  }

  @Test
  void approve_candidate_completes() {
    MenteeCompletion mc = new MenteeCompletion(10L, 2L);
    mc.applyJudgement(4, 5); // COMPLETION_CANDIDATE
    when(completionRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.of(mc));
    when(completionRepository.save(any(MenteeCompletion.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    MenteeCompletionResponse res =
        completionService.approveMenteeCompletion(admin, 10L, 2L);

    assertThat(res.status()).isEqualTo(CompletionStatus.COMPLETED);
    assertThat(res.approvedAt()).isNotNull();
  }

  @Test
  void approve_notEligible_conflict409() {
    MenteeCompletion mc = new MenteeCompletion(10L, 2L);
    mc.applyJudgement(2, 5); // NOT_COMPLETED
    when(completionRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.of(mc));

    assertThatThrownBy(() -> completionService.approveMenteeCompletion(admin, 10L, 2L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.COMPLETION_NOT_ELIGIBLE);
  }

  @Test
  void approve_alreadyCompleted_conflict409_checkedFirst() {
    MenteeCompletion mc = new MenteeCompletion(10L, 2L);
    mc.applyJudgement(4, 5);
    mc.approve(java.time.OffsetDateTime.now()); // COMPLETED
    when(completionRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.of(mc));

    assertThatThrownBy(() -> completionService.approveMenteeCompletion(admin, 10L, 2L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.COMPLETION_ALREADY_APPROVED);
  }

  @Test
  void approve_missing_notFound404() {
    when(completionRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> completionService.approveMenteeCompletion(admin, 10L, 2L))
        .isInstanceOf(NotFoundException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.COMPLETION_NOT_FOUND);
  }

  @Test
  void approve_nonAdmin_forbidden403() {
    assertThatThrownBy(() -> completionService.approveMenteeCompletion(owningMentor, 10L, 2L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.COMPLETION_FORBIDDEN);
  }

  @Test
  void getCompletions_admin_returnsList() {
    MenteeCompletion mc = new MenteeCompletion(10L, 2L);
    mc.applyJudgement(4, 5);
    when(completionRepository.findByMeetingId(10L)).thenReturn(List.of(mc));

    List<MenteeCompletionResponse> res = completionService.getCompletions(admin, 10L);

    assertThat(res).hasSize(1);
  }
}
