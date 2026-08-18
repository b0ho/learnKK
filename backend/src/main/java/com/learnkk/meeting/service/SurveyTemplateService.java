package com.learnkk.meeting.service;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.meeting.dto.SurveyQuestionDto;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.entity.SurveyQuestion;
import com.learnkk.meeting.repository.MeetingRepository;
import com.learnkk.meeting.repository.SurveyQuestionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the pre-application survey template attached to a meeting. */
@Service
public class SurveyTemplateService {

  private final MeetingRepository meetingRepository;
  private final SurveyQuestionRepository questionRepository;

  public SurveyTemplateService(
      MeetingRepository meetingRepository, SurveyQuestionRepository questionRepository) {
    this.meetingRepository = meetingRepository;
    this.questionRepository = questionRepository;
  }

  /** Replaces all questions for a meeting. Only the owning mentor may edit before IN_PROGRESS. */
  @Transactional
  public List<SurveyQuestionDto> upsertQuestions(
      Long mentorId, Long meetingId, List<SurveyQuestionDto> questions) {
    Meeting meeting = loadMeeting(meetingId);
    if (!meeting.getMentorId().equals(mentorId)) {
      throw new ForbiddenException(ErrorCodes.MEETING_FORBIDDEN, "모임 소유 멘토만 문항을 수정할 수 있습니다.");
    }
    if (isLocked(meeting.getStatus())) {
      throw new ConflictException(
          ErrorCodes.MEETING_QUESTIONS_LOCKED, "진행 중 이후 모임의 문항은 수정할 수 없습니다.");
    }

    questionRepository.deleteByMeetingId(meetingId);
    List<SurveyQuestion> entities =
        questions.stream()
            .map(
                q ->
                    new SurveyQuestion(
                        meetingId,
                        q.orderNo(),
                        q.text(),
                        q.type(),
                        q.options(),
                        q.required() == null || q.required()))
            .toList();
    questionRepository.saveAll(entities);
    return getQuestions(meetingId);
  }

  @Transactional(readOnly = true)
  public List<SurveyQuestionDto> getQuestions(Long meetingId) {
    return questionRepository.findByMeetingIdOrderByOrderNoAsc(meetingId).stream()
        .map(SurveyQuestionDto::from)
        .toList();
  }

  private boolean isLocked(MeetingStatus status) {
    // Locked once recruitment/meeting has progressed to IN_PROGRESS or beyond.
    return status == MeetingStatus.IN_PROGRESS
        || status == MeetingStatus.COMPLETED
        || status == MeetingStatus.CANCELLED;
  }

  private Meeting loadMeeting(Long meetingId) {
    return meetingRepository
        .findById(meetingId)
        .orElseThrow(
            () ->
                new com.learnkk.kernel.error.NotFoundException(
                    ErrorCodes.MEETING_NOT_FOUND, "모임을 찾을 수 없습니다."));
  }
}
