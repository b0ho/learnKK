package com.learnkk.content.service;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import org.springframework.stereotype.Service;

/**
 * Cross-unit authorization for U6 content. Composes U3 (mentor ownership) and U4 (participant
 * enrollment) reads via their Service interfaces — never touching those modules' tables (ADR-003).
 * The U6→U3 and U6→U4 read edges are acyclic (neither U3 nor U4 reads U6).
 */
@Service
public class ContentAccessService {

  private final MeetingService meetingService;
  private final EnrollmentService enrollmentService;

  public ContentAccessService(MeetingService meetingService, EnrollmentService enrollmentService) {
    this.meetingService = meetingService;
    this.enrollmentService = enrollmentService;
  }

  /** Returns the meeting after asserting the caller owns it as mentor; else 403 (BR-U6-1/5). */
  public MeetingResponse assertOwningMentor(Principal principal, Long meetingId) {
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    boolean owningMentor = principal.isMentor() && meeting.mentorId().equals(principal.userId());
    if (!owningMentor) {
      throw new ForbiddenException(ErrorCodes.CONTENT_FORBIDDEN, "이 모임의 자료를 작성할 권한이 없습니다.");
    }
    return meeting;
  }

  /**
   * Assert the caller may view a meeting's content: owning mentor, APPLIED mentee or admin
   * (BR-U6-3). Non-participants get 403 {@code CONTENT_FORBIDDEN}. Also serves to 404 the meeting
   * if it does not exist (via {@code getMeeting}).
   */
  public void assertParticipant(Principal principal, Long meetingId) {
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    boolean owningMentor = principal.isMentor() && meeting.mentorId().equals(principal.userId());
    boolean participant =
        owningMentor
            || principal.isAdmin()
            || enrollmentService.isParticipant(meetingId, principal.userId());
    if (!participant) {
      throw new ForbiddenException(ErrorCodes.CONTENT_FORBIDDEN, "이 모임의 자료를 열람할 권한이 없습니다.");
    }
  }
}
