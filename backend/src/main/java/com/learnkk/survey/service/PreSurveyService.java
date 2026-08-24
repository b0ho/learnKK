package com.learnkk.survey.service;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Pre-application survey answers (U8, C7). Submission is gated to IN_PROGRESS meetings (BR-U8-1);
 * reads are authorization-only (owning mentor / admin / self) with no status gate (BR-U8-2). U3
 * (meeting status + question template) and U4 (participant) are read via their services — U8 never
 * touches those tables directly.
 */
@Service
public class PreSurveyService {

  private final MeetingService meetingService;
  private final SurveyTemplateService surveyTemplateService;
  private final EnrollmentService enrollmentService;
  private final SurveyAnswerRepository answerRepository;

  public PreSurveyService(
      MeetingService meetingService,
      SurveyTemplateService surveyTemplateService,
      EnrollmentService enrollmentService,
      SurveyAnswerRepository answerRepository) {
    this.meetingService = meetingService;
    this.surveyTemplateService = surveyTemplateService;
    this.enrollmentService = enrollmentService;
    this.answerRepository = answerRepository;
  }

  /**
   * Submit (or re-submit) a mentee's pre-application survey answers (US-3.6, W1). Only
   * participating mentees may submit, only while the meeting is IN_PROGRESS, and only if every
   * required question is answered. Answers are upserted per (question, mentee).
   */
  @Transactional
  public List<SurveyAnswerResponse> submitAnswers(
      Principal principal, Long meetingId, SurveyAnswerRequest request) {
    Long menteeId = principal.userId();
    if (!enrollmentService.isActiveParticipant(meetingId, menteeId)) {
      throw new ForbiddenException(ErrorCodes.PRESURVEY_FORBIDDEN, "참여 중인 모임에만 응답할 수 있습니다.");
    }

    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    if (meeting.status() != MeetingStatus.IN_PROGRESS) {
      throw new ConflictException(ErrorCodes.PRESURVEY_NOT_OPEN, "모임 시작 이후에만 사전설문에 응답할 수 있습니다.");
    }

    List<SurveyQuestionDto> questions = surveyTemplateService.getQuestions(meetingId);
    Map<Long, String> answersByQuestion = new HashMap<>();
    for (SurveyAnswerRequest.AnswerItem item : request.answers()) {
      answersByQuestion.put(item.questionId(), item.answerText());
    }

    for (SurveyQuestionDto question : questions) {
      boolean required = question.required() == null || question.required();
      if (required && !StringUtils.hasText(answersByQuestion.get(question.id()))) {
        throw new ValidationException(ErrorCodes.PRESURVEY_REQUIRED_MISSING, "필수 문항에 모두 응답해야 합니다.");
      }
    }

    for (SurveyQuestionDto question : questions) {
      if (!answersByQuestion.containsKey(question.id())) {
        continue;
      }
      String answerText = answersByQuestion.get(question.id());
      answerRepository
          .findByQuestionIdAndMenteeId(question.id(), menteeId)
          .ifPresentOrElse(
              existing -> existing.updateAnswer(answerText),
              () ->
                  answerRepository.save(
                      new SurveyAnswer(meetingId, question.id(), menteeId, answerText)));
    }

    return answerRepository.findByMeetingIdAndMenteeId(meetingId, menteeId).stream()
        .map(SurveyAnswerResponse::from)
        .toList();
  }

  /**
   * Read a mentee's pre-application survey answers (W2). Allowed for the owning mentor, an admin,
   * or the mentee themselves. No status gate — mentor/admin can read after the meeting completes.
   */
  @Transactional(readOnly = true)
  public List<SurveyAnswerResponse> getAnswers(Principal principal, Long meetingId, Long menteeId) {
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    boolean owningMentor = principal.isMentor() && meeting.mentorId().equals(principal.userId());
    boolean self = menteeId.equals(principal.userId());
    if (!owningMentor && !principal.isAdmin() && !self) {
      throw new ForbiddenException(ErrorCodes.PRESURVEY_FORBIDDEN, "응답을 조회할 권한이 없습니다.");
    }

    return answerRepository.findByMeetingIdAndMenteeId(meetingId, menteeId).stream()
        .map(SurveyAnswerResponse::from)
        .toList();
  }
}
