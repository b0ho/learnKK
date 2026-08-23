package com.learnkk.survey.service;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.survey.dto.FeedbackResponse;
import com.learnkk.survey.entity.Feedback;
import com.learnkk.survey.repository.FeedbackRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Course feedback (U8, C7). Submission is restricted to participating mentees while the meeting is
 * IN_PROGRESS or COMPLETED (BR-U8-3); reads are restricted to the owning mentor or an admin — other
 * mentors get 403 and mentees have no read path (BR-U8-4). U3 (status + owning mentor) and U4
 * (participant) are read via their services.
 */
@Service
public class FeedbackService {

  /** Meeting states in which a participating mentee may submit feedback (BR-U8-3). */
  private static final Set<MeetingStatus> SUBMITTABLE =
      EnumSet.of(MeetingStatus.IN_PROGRESS, MeetingStatus.COMPLETED);

  private final MeetingService meetingService;
  private final EnrollmentService enrollmentService;
  private final FeedbackRepository feedbackRepository;

  public FeedbackService(
      MeetingService meetingService,
      EnrollmentService enrollmentService,
      FeedbackRepository feedbackRepository) {
    this.meetingService = meetingService;
    this.enrollmentService = enrollmentService;
    this.feedbackRepository = feedbackRepository;
  }

  /**
   * Submit (or re-submit) course feedback (US-8.1, W3). Only participating mentees may submit, only
   * while the meeting is IN_PROGRESS or COMPLETED. Feedback is upserted per (meeting, mentee).
   */
  @Transactional
  public FeedbackResponse submitFeedback(Principal principal, Long meetingId, String content) {
    Long menteeId = principal.userId();
    if (!enrollmentService.isActiveParticipant(meetingId, menteeId)) {
      throw new ForbiddenException(ErrorCodes.FEEDBACK_FORBIDDEN, "참여 중인 모임에만 피드백을 제출할 수 있습니다.");
    }

    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    if (!SUBMITTABLE.contains(meeting.status())) {
      throw new ConflictException(
          ErrorCodes.FEEDBACK_NOT_OPEN, "진행 중이거나 완료된 모임에만 피드백을 제출할 수 있습니다.");
    }

    Feedback feedback =
        feedbackRepository
            .findByMeetingIdAndMenteeId(meetingId, menteeId)
            .map(
                existing -> {
                  existing.updateContent(content);
                  return existing;
                })
            .orElseGet(() -> new Feedback(meetingId, menteeId, content));
    return FeedbackResponse.from(feedbackRepository.save(feedback));
  }

  /**
   * List a meeting's feedback (US-8.2, W4). Restricted to the owning mentor or an admin; any other
   * caller (including other mentors and mentees) gets 403.
   */
  @Transactional(readOnly = true)
  public List<FeedbackResponse> listFeedback(Principal principal, Long meetingId) {
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    boolean owningMentor = principal.isMentor() && meeting.mentorId().equals(principal.userId());
    if (!owningMentor && !principal.isAdmin()) {
      throw new ForbiddenException(ErrorCodes.FEEDBACK_FORBIDDEN, "피드백을 조회할 권한이 없습니다.");
    }

    return feedbackRepository.findByMeetingId(meetingId).stream()
        .map(FeedbackResponse::from)
        .toList();
  }
}
