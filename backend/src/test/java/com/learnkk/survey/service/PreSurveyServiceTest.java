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
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.SurveyQuestionDto;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.meeting.service.SurveyTemplateService;
import com.learnkk.survey.dto.SurveyAnswerRequest;
import com.learnkk.survey.dto.SurveyAnswerResponse;
import com.learnkk.survey.entity.SurveyAnswer;
import com.learnkk.survey.repository.SurveyAnswerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreSurveyServiceTest {

  @Mock private MeetingService meetingService;
  @Mock private SurveyTemplateService surveyTemplateService;
  @Mock private EnrollmentService enrollmentService;
  @Mock private SurveyAnswerRepository answerRepository;

  @InjectMocks private PreSurveyService preSurveyService;

  private final Principal mentee = new Principal(2L, Role.MENTEE);
  private final Principal owningMentor = new Principal(1L, Role.MENTOR);
  private final Principal otherMentor = new Principal(5L, Role.MENTOR);
  private final Principal admin = new Principal(9L, Role.ADMIN);

  private MeetingResponse meeting(MeetingStatus status) {
    return new MeetingResponse(
        10L, 1L, "Spring", "backend", 8, null, null, 5, "online", "intro", status, null, null);
  }

  private SurveyQuestionDto question(Long id, boolean required) {
    return new SurveyQuestionDto(id, 1, "질문" + id, "TEXT", List.of(), required);
  }

  private SurveyAnswerRequest request(Long questionId, String text) {
    return new SurveyAnswerRequest(List.of(new SurveyAnswerRequest.AnswerItem(questionId, text)));
  }

  // --- submitAnswers ---

  @Test
  void submitAnswers_happyPath_upsertsAndReturns() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(surveyTemplateService.getQuestions(10L)).thenReturn(List.of(question(100L, true)));
    when(answerRepository.findByQuestionIdAndMenteeId(100L, 2L)).thenReturn(Optional.empty());
    when(answerRepository.findByMeetingIdAndMenteeId(10L, 2L))
        .thenReturn(List.of(new SurveyAnswer(10L, 100L, 2L, "답변")));

    List<SurveyAnswerResponse> result =
        preSurveyService.submitAnswers(mentee, 10L, request(100L, "답변"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).questionId()).isEqualTo(100L);
    verify(answerRepository).save(any(SurveyAnswer.class));
  }

  @Test
  void submitAnswers_beforeStart_conflict409NotOpen() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.READY_TO_START));

    assertThatThrownBy(() -> preSurveyService.submitAnswers(mentee, 10L, request(100L, "답변")))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.PRESURVEY_NOT_OPEN);
    verify(answerRepository, never()).save(any());
  }

  @Test
  void submitAnswers_nonParticipant_forbidden403() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(false);

    assertThatThrownBy(() -> preSurveyService.submitAnswers(mentee, 10L, request(100L, "답변")))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.PRESURVEY_FORBIDDEN);
    verify(meetingService, never()).getMeeting(anyLong());
  }

  @Test
  void submitAnswers_requiredMissing_validation400() {
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(surveyTemplateService.getQuestions(10L)).thenReturn(List.of(question(100L, true)));

    // Answer is blank for a required question.
    assertThatThrownBy(() -> preSurveyService.submitAnswers(mentee, 10L, request(100L, "  ")))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.PRESURVEY_REQUIRED_MISSING);
    verify(answerRepository, never()).save(any());
  }

  @Test
  void submitAnswers_reSubmit_updatesInPlace() {
    SurveyAnswer existing = new SurveyAnswer(10L, 100L, 2L, "이전");
    when(enrollmentService.isActiveParticipant(10L, 2L)).thenReturn(true);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(surveyTemplateService.getQuestions(10L)).thenReturn(List.of(question(100L, true)));
    when(answerRepository.findByQuestionIdAndMenteeId(100L, 2L)).thenReturn(Optional.of(existing));
    when(answerRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(List.of(existing));

    preSurveyService.submitAnswers(mentee, 10L, request(100L, "새답변"));

    assertThat(existing.getAnswerText()).isEqualTo("새답변");
    verify(answerRepository, never()).save(any());
  }

  // --- getAnswers ---

  @Test
  void getAnswers_owningMentor_returns200() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(answerRepository.findByMeetingIdAndMenteeId(10L, 2L))
        .thenReturn(List.of(new SurveyAnswer(10L, 100L, 2L, "답변")));

    assertThat(preSurveyService.getAnswers(owningMentor, 10L, 2L)).hasSize(1);
  }

  @Test
  void getAnswers_admin_returns200() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.COMPLETED));
    when(answerRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(List.of());

    assertThat(preSurveyService.getAnswers(admin, 10L, 2L)).isEmpty();
  }

  @Test
  void getAnswers_self_returns200() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));
    when(answerRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(List.of());

    assertThat(preSurveyService.getAnswers(mentee, 10L, 2L)).isEmpty();
  }

  @Test
  void getAnswers_afterCompleted_mentorStillReads() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.COMPLETED));
    when(answerRepository.findByMeetingIdAndMenteeId(10L, 2L))
        .thenReturn(List.of(new SurveyAnswer(10L, 100L, 2L, "답변")));

    // No status gate on read: owning mentor reads after COMPLETED.
    assertThat(preSurveyService.getAnswers(owningMentor, 10L, 2L)).hasSize(1);
  }

  @Test
  void getAnswers_otherMentor_forbidden403() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS));

    assertThatThrownBy(() -> preSurveyService.getAnswers(otherMentor, 10L, 2L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.PRESURVEY_FORBIDDEN);
  }
}
