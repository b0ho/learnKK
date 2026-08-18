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

  @InjectMocks private MeetingApprovalService approvalService;

  private final Principal admin = new Principal(9L, Role.ADMIN);
  private final Principal mentor = new Principal(1L, Role.MENTOR);

  private Meeting recruitingMeeting() {
    Meeting m = new Meeting(1L, "t", null, 4, null, null, 5, null, null);
    m.setStatus(MeetingStatus.RECRUITING);
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
}
