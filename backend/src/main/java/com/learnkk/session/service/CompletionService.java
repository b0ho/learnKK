package com.learnkk.session.service;

import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.CompletionStatus;
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
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 80% 수료 자동 판정(W3, BR-U5-4) + ④ 관리자 확정(W4, BR-U5-5). 판정식은 정수 연산 {@code a*100 >= 80*S} 로
 * 부동소수 오차를 피한다. 수료 대상 멘티 집합은 {@link EnrollmentService} 무권한 read(U5→U4)로 확정한다.
 */
@Service
public class CompletionService {

  private final MenteeCompletionRepository completionRepository;
  private final MeetingSessionRepository sessionRepository;
  private final AttendanceRepository attendanceRepository;
  private final EnrollmentService enrollmentService;
  private final MeetingService meetingService;

  public CompletionService(
      MenteeCompletionRepository completionRepository,
      MeetingSessionRepository sessionRepository,
      AttendanceRepository attendanceRepository,
      EnrollmentService enrollmentService,
      MeetingService meetingService) {
    this.completionRepository = completionRepository;
    this.sessionRepository = sessionRepository;
    this.attendanceRepository = attendanceRepository;
    this.enrollmentService = enrollmentService;
    this.meetingService = meetingService;
  }

  /**
   * 참여 멘티별 80% 수료 판정(US-7.1). 소유 멘토/관리자만 실행 가능(403). S=전체 예정 세션 수, 각 멘티 a=출석 세션 수.
   * S&gt;0 이고 {@code a*100 >= 80*S} 이면 COMPLETION_CANDIDATE, 아니면 NOT_COMPLETED(S=0 이면 후보 보류).
   * 이미 확정(COMPLETED)된 건은 재판정하지 않는다.
   */
  @Transactional
  public List<MenteeCompletionResponse> computeCompletion(Principal principal, Long meetingId) {
    requireOwningMentorOrAdmin(principal, meetingId);

    int scheduled = sessionRepository.countByMeetingId(meetingId);
    List<Long> participants = enrollmentService.listActiveMenteeIds(meetingId);

    for (Long menteeId : participants) {
      int attended = (int) attendanceRepository.countAttendedSessions(meetingId, menteeId);
      MenteeCompletion mc =
          completionRepository
              .findByMeetingIdAndMenteeId(meetingId, menteeId)
              .orElseGet(() -> new MenteeCompletion(meetingId, menteeId));
      mc.applyJudgement(attended, scheduled);
      completionRepository.save(mc);
    }
    return getCompletions(principal, meetingId);
  }

  /** 수료 판정 결과 조회(US-7.4). 소유 멘토/관리자만 가능(403). */
  @Transactional(readOnly = true)
  public List<MenteeCompletionResponse> getCompletions(Principal principal, Long meetingId) {
    requireOwningMentorOrAdmin(principal, meetingId);
    return completionRepository.findByMeetingId(meetingId).stream()
        .map(MenteeCompletionResponse::from)
        .toList();
  }

  /**
   * ④ 관리자 수료 확정(US-7.2). ADMIN 만 가능(403). 이미 COMPLETED 면 409 ALREADY_APPROVED(먼저 검사, 리뷰
   * S2), COMPLETION_CANDIDATE 가 아니면 409 NOT_ELIGIBLE. 확정 시 status=COMPLETED, approvedAt 기록(스냅샷
   * 유지).
   */
  @Transactional
  public MenteeCompletionResponse approveMenteeCompletion(
      Principal principal, Long meetingId, Long menteeId) {
    if (!principal.isAdmin()) {
      throw new ForbiddenException(ErrorCodes.COMPLETION_FORBIDDEN, "관리자만 수료를 확정할 수 있습니다.");
    }
    MenteeCompletion mc =
        completionRepository
            .findByMeetingIdAndMenteeId(meetingId, menteeId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        ErrorCodes.COMPLETION_NOT_FOUND, "수료 판정 내역을 찾을 수 없습니다."));

    if (mc.getStatus() == CompletionStatus.COMPLETED) {
      throw new ConflictException(ErrorCodes.COMPLETION_ALREADY_APPROVED, "이미 수료 확정된 멘티입니다.");
    }
    if (mc.getStatus() != CompletionStatus.COMPLETION_CANDIDATE) {
      throw new ConflictException(ErrorCodes.COMPLETION_NOT_ELIGIBLE, "수료 기준을 충족하지 않았습니다.");
    }

    mc.approve(OffsetDateTime.now());
    return MenteeCompletionResponse.from(completionRepository.save(mc));
  }

  private void requireOwningMentorOrAdmin(Principal principal, Long meetingId) {
    if (principal.isAdmin()) {
      return;
    }
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    boolean owningMentor =
        principal.isMentor() && meeting.mentorId().equals(principal.userId());
    if (!owningMentor) {
      throw new ForbiddenException(ErrorCodes.COMPLETION_FORBIDDEN, "수료 현황을 조회할 권한이 없습니다.");
    }
  }
}
