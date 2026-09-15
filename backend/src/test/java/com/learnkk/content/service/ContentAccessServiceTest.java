package com.learnkk.content.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentAccessServiceTest {

  @Mock private MeetingService meetingService;
  @Mock private EnrollmentService enrollmentService;

  @InjectMocks private ContentAccessService accessService;

  private final Principal owningMentor = new Principal(1L, Role.MENTOR);
  private final Principal otherMentor = new Principal(5L, Role.MENTOR);
  private final Principal mentee = new Principal(2L, Role.MENTEE);
  private final Principal admin = new Principal(9L, Role.ADMIN);

  private MeetingResponse meeting() {
    return new MeetingResponse(
        10L,
        1L,
        "Spring",
        "backend",
        8,
        null,
        null,
        5,
        "online",
        "intro",
        MeetingStatus.IN_PROGRESS,
        null,
        null);
  }

  // --- assertOwningMentor ---

  @Test
  void assertOwningMentor_owner_ok() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    assertThatCode(() -> accessService.assertOwningMentor(owningMentor, 10L))
        .doesNotThrowAnyException();
  }

  @Test
  void assertOwningMentor_otherMentor_forbidden() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    assertThatThrownBy(() -> accessService.assertOwningMentor(otherMentor, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.CONTENT_FORBIDDEN);
  }

  @Test
  void assertOwningMentor_admin_forbidden() {
    // Admin is not the owning mentor — writing content is mentor-only (BR-U6-1/5).
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    assertThatThrownBy(() -> accessService.assertOwningMentor(admin, 10L))
        .isInstanceOf(ForbiddenException.class);
  }

  // --- assertParticipant ---

  @Test
  void assertParticipant_owningMentor_ok() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    assertThatCode(() -> accessService.assertParticipant(owningMentor, 10L))
        .doesNotThrowAnyException();
  }

  @Test
  void assertParticipant_appliedMentee_ok() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    when(enrollmentService.isParticipant(10L, 2L)).thenReturn(true);
    assertThatCode(() -> accessService.assertParticipant(mentee, 10L)).doesNotThrowAnyException();
  }

  @Test
  void assertParticipant_admin_ok() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    assertThatCode(() -> accessService.assertParticipant(admin, 10L)).doesNotThrowAnyException();
  }

  @Test
  void assertParticipant_nonParticipantMentee_forbidden() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    when(enrollmentService.isParticipant(10L, 2L)).thenReturn(false);
    assertThatThrownBy(() -> accessService.assertParticipant(mentee, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.CONTENT_FORBIDDEN);
  }

  @Test
  void assertParticipant_nonOwningMentor_forbidden() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting());
    when(enrollmentService.isParticipant(10L, 5L)).thenReturn(false);
    assertThatThrownBy(() -> accessService.assertParticipant(otherMentor, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.CONTENT_FORBIDDEN);
  }
}
