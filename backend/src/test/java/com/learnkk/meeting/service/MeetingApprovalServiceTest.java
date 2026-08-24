package com.learnkk.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeetingApprovalServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private SessionCompletionGate sessionCompletionGate;

  @InjectMocks private MeetingApprovalService approvalService;

  private final Principal admin = new Principal(9L, Role.ADMIN);
  private final Principal mentor = new Principal(1L, Role.MENTOR);

  private Meeting recruitingMeeting() {
    Meeting m = new Meeting(1L, "t", null, 4, null, null, 5, null, null);
    m.setStatus(MeetingStatus.RECRUITING);
    return m;
  }

  private Meeting withStatus(MeetingStatus status) {
    Meeting m = new Meeting(1L, "t", null, 4, null, null, 5, null, null);
    m.setStatus(status);
    return m;
  }

  @Test
  void approve_happyPath_transitionsToRecruiting() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.PENDING_APPROVAL), eq(MeetingStatus.RECRUITING), isNull()))
        .thenReturn(1);
    when(meetingRepository.findById(1L)).thenReturn(Optional.of(recruitingMeeting()));

    MeetingResponse response = approvalService.approveCreation(admin, 1L);

    assertThat(response.status()).isEqualTo(MeetingStatus.RECRUITING);
  }

  @Test
  void approve_illegalState_conflict409() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.PENDING_APPROVAL), eq(MeetingStatus.RECRUITING), isNull()))
        .thenReturn(0);

    assertThatThrownBy(() -> approvalService.approveCreation(admin, 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  @Test
  void approve_doubleApprove_conflict409() {
    // Second approval attempt: the conditional UPDATE affects 0 rows.
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.PENDING_APPROVAL), eq(MeetingStatus.RECRUITING), isNull()))
        .thenReturn(0);

    assertThatThrownBy(() -> approvalService.approveCreation(admin, 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  @Test
  void approve_nonAdmin_forbidden403() {
    assertThatThrownBy(() -> approvalService.approveCreation(mentor, 1L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
  }

  @Test
  void reject_happyPath_transitionsToRejected() {
    Meeting rejected = new Meeting(1L, "t", null, 4, null, null, 5, null, null);
    rejected.setStatus(MeetingStatus.REJECTED);
    rejected.setRejectReason("주제 부적합");
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.PENDING_APPROVAL), eq(MeetingStatus.REJECTED), eq("주제 부적합")))
        .thenReturn(1);
    when(meetingRepository.findById(1L)).thenReturn(Optional.of(rejected));

    MeetingResponse response = approvalService.rejectCreation(admin, 1L, "주제 부적합");

    assertThat(response.status()).isEqualTo(MeetingStatus.REJECTED);
    assertThat(response.rejectReason()).isEqualTo("주제 부적합");
  }

  @Test
  void reject_illegalState_conflict409() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.PENDING_APPROVAL), eq(MeetingStatus.REJECTED), eq("x")))
        .thenReturn(0);

    assertThatThrownBy(() -> approvalService.rejectCreation(admin, 1L, "x"))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  // --- T3: confirm recruitment (proceed) ---

  @Test
  void confirmRecruitment_proceed_transitionsToReadyToStart() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.RECRUITING), eq(MeetingStatus.READY_TO_START), isNull()))
        .thenReturn(1);
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(withStatus(MeetingStatus.READY_TO_START)));

    MeetingResponse response = approvalService.confirmRecruitment(admin, 1L, true, null);

    assertThat(response.status()).isEqualTo(MeetingStatus.READY_TO_START);
  }

  @Test
  void confirmRecruitment_notRecruiting_conflict409() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.RECRUITING), eq(MeetingStatus.READY_TO_START), isNull()))
        .thenReturn(0);

    assertThatThrownBy(() -> approvalService.confirmRecruitment(admin, 1L, true, null))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  @Test
  void confirmRecruitment_nonAdmin_forbidden403() {
    assertThatThrownBy(() -> approvalService.confirmRecruitment(mentor, 1L, true, null))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
  }

  // --- T4: cancel recruitment (proceed=false) ---

  @Test
  void confirmRecruitment_cancel_transitionsToCancelledWithReason() {
    Meeting cancelled = withStatus(MeetingStatus.CANCELLED);
    cancelled.setRejectReason("정원 미달");
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.RECRUITING), eq(MeetingStatus.CANCELLED), eq("정원 미달")))
        .thenReturn(1);
    when(meetingRepository.findById(1L)).thenReturn(Optional.of(cancelled));

    MeetingResponse response = approvalService.confirmRecruitment(admin, 1L, false, "정원 미달");

    assertThat(response.status()).isEqualTo(MeetingStatus.CANCELLED);
    assertThat(response.rejectReason()).isEqualTo("정원 미달");
  }

  @Test
  void confirmRecruitment_cancelWithoutReason_validation400() {
    when(meetingRepository.existsById(1L)).thenReturn(true);

    assertThatThrownBy(() -> approvalService.confirmRecruitment(admin, 1L, false, "  "))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_VALIDATION);
  }

  // --- T5: approve start ---

  @Test
  void approveStart_happyPath_transitionsToInProgress() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.READY_TO_START), eq(MeetingStatus.IN_PROGRESS), isNull()))
        .thenReturn(1);
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(withStatus(MeetingStatus.IN_PROGRESS)));

    MeetingResponse response = approvalService.approveStart(admin, 1L);

    assertThat(response.status()).isEqualTo(MeetingStatus.IN_PROGRESS);
  }

  @Test
  void approveStart_notReady_conflict409() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.READY_TO_START), eq(MeetingStatus.IN_PROGRESS), isNull()))
        .thenReturn(0);

    assertThatThrownBy(() -> approvalService.approveStart(admin, 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  @Test
  void approveStart_nonAdmin_forbidden403() {
    assertThatThrownBy(() -> approvalService.approveStart(mentor, 1L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
  }

  // --- T6: complete ---

  @Test
  void completeMeeting_happyPath_transitionsToCompleted() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(sessionCompletionGate.allScheduledSessionsEnded(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.IN_PROGRESS), eq(MeetingStatus.COMPLETED), isNull()))
        .thenReturn(1);
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(withStatus(MeetingStatus.COMPLETED)));

    MeetingResponse response = approvalService.completeMeeting(admin, 1L);

    assertThat(response.status()).isEqualTo(MeetingStatus.COMPLETED);
  }

  @Test
  void completeMeeting_sessionsNotEnded_conflict409() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(sessionCompletionGate.allScheduledSessionsEnded(1L)).thenReturn(false);

    assertThatThrownBy(() -> approvalService.completeMeeting(admin, 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_SESSIONS_NOT_ENDED);
  }

  @Test
  void completeMeeting_notInProgress_conflict409() {
    when(meetingRepository.existsById(1L)).thenReturn(true);
    when(sessionCompletionGate.allScheduledSessionsEnded(1L)).thenReturn(true);
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.IN_PROGRESS), eq(MeetingStatus.COMPLETED), isNull()))
        .thenReturn(0);

    assertThatThrownBy(() -> approvalService.completeMeeting(admin, 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  @Test
  void completeMeeting_nonAdmin_forbidden403() {
    assertThatThrownBy(() -> approvalService.completeMeeting(mentor, 1L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
  }

  // --- FR-5: 승인 되돌리기(역전이) ---

  @Test
  void revert_recruiting_toPendingApproval() {
    when(meetingRepository.findById(1L))
        .thenReturn(
            Optional.of(withStatus(MeetingStatus.RECRUITING)),
            Optional.of(withStatus(MeetingStatus.PENDING_APPROVAL)));
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.RECRUITING), eq(MeetingStatus.PENDING_APPROVAL), isNull()))
        .thenReturn(1);

    MeetingResponse response = approvalService.revert(admin, 1L);

    assertThat(response.status()).isEqualTo(MeetingStatus.PENDING_APPROVAL);
  }

  @Test
  void revert_completed_toInProgress() {
    when(meetingRepository.findById(1L))
        .thenReturn(
            Optional.of(withStatus(MeetingStatus.COMPLETED)),
            Optional.of(withStatus(MeetingStatus.IN_PROGRESS)));
    when(meetingRepository.transitionStatus(
            eq(1L), eq(MeetingStatus.COMPLETED), eq(MeetingStatus.IN_PROGRESS), isNull()))
        .thenReturn(1);

    MeetingResponse response = approvalService.revert(admin, 1L);

    assertThat(response.status()).isEqualTo(MeetingStatus.IN_PROGRESS);
  }

  @Test
  void revert_pendingApproval_notRevertible_conflict409() {
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(withStatus(MeetingStatus.PENDING_APPROVAL)));

    assertThatThrownBy(() -> approvalService.revert(admin, 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  @Test
  void revert_rejected_notRevertible_conflict409() {
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(withStatus(MeetingStatus.REJECTED)));

    assertThatThrownBy(() -> approvalService.revert(admin, 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_INVALID_TRANSITION);
  }

  @Test
  void revert_nonAdmin_forbidden403() {
    assertThatThrownBy(() -> approvalService.revert(mentor, 1L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
  }
}
