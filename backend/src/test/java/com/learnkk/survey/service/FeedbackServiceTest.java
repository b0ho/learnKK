package com.learnkk.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.survey.dto.FeedbackResponse;
import com.learnkk.survey.entity.Feedback;
import com.learnkk.survey.repository.FeedbackRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

  @Mock private MeetingService meetingService;
  @Mock private EnrollmentService enrollmentService;
  @Mock private FeedbackRepository feedbackRepository;

  @InjectMocks private FeedbackService feedbackService;

  private final Principal mentee = new Principal(2L, Role.MENTEE);
  private final Principal owningMentor = new Principal(1L, Role.MENTOR);
  private final Principal otherMentor = new Principal(5L, Role.MENTOR);
  private final Principal admin = new Principal(9L, Role.ADMIN);

  private MeetingResponse meeting(MeetingStatus status) {
    return new MeetingResponse(
        10L, 1L, "Spring", "backend", 8, null, null, 5, "online", "intro", status, null, null);
  }

  // --- submitFeedback ---

  @Test
  void submitFeedback_happyPath_returnsSaved() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(feedbackRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

    FeedbackResponse response = feedbackService.submitFeedback(mentee, 10L, "좋았습니다");

    assertThat(response.content()).isEqualTo("좋았습니다");
    assertThat(response.menteeId()).isEqualTo(2L);
  }

  @Test
  void submitFeedback_completedMeeting_allowed() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.COMPLETED));
    when(feedbackRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

    assertThat(feedbackService.submitFeedback(mentee, 10L, "완료 후 제출").content())
        .isEqualTo("완료 후 제출");
  }

  @Test
  void submitFeedback_nonParticipant_forbidden403() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(false);

    assertThatThrownBy(() -> feedbackService.submitFeedback(mentee, 10L, "내용"))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.FEEDBACK_FORBIDDEN);
    verify(meetingService, never()).getMeeting(anyLong());
  }

  @Test
  void submitFeedback_notInSubmittableStatus_conflict409NotOpen() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING));

    assertThatThrownBy(() -> feedbackService.submitFeedback(mentee, 10L, "내용"))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.FEEDBACK_NOT_OPEN);
    verify(feedbackRepository, never()).save(any());
  }

  @Test
  void submitFeedback_reSubmit_updatesInPlace() {
    Feedback existing = new Feedback(10L, 2L, "이전");
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(feedbackRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.of(existing));
    when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

    feedbackService.submitFeedback(mentee, 10L, "새 내용");

    assertThat(existing.getContent()).isEqualTo("새 내용");
  }

  // --- listFeedback ---

  @Test
  void listFeedback_owningMentor_returns200() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.COMPLETED));
    when(feedbackRepository.findByMeetingId(10L)).thenReturn(List.of(new Feedback(10L, 2L, "내용")));

    assertThat(feedbackService.listFeedback(owningMentor, 10L)).hasSize(1);
  }

  @Test
  void listFeedback_admin_returns200() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.COMPLETED));
    when(feedbackRepository.findByMeetingId(10L)).thenReturn(List.of());

    assertThat(feedbackService.listFeedback(admin, 10L)).isEmpty();
  }

  @Test
  void listFeedback_otherMentor_forbidden403() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.COMPLETED));

    assertThatThrownBy(() -> feedbackService.listFeedback(otherMentor, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.FEEDBACK_FORBIDDEN);
  }

  @Test
  void listFeedback_mentee_forbidden403() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.COMPLETED));

    assertThatThrownBy(() -> feedbackService.listFeedback(mentee, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.FEEDBACK_FORBIDDEN);
  }
}
