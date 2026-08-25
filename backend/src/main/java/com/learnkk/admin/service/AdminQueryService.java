package com.learnkk.admin.service;

import com.learnkk.admin.dto.ApprovalQueues;
import com.learnkk.admin.dto.MeetingMonitorRow;
import com.learnkk.admin.dto.MeetingQueueItem;
import com.learnkk.admin.dto.MenteeCompletionQueueItem;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.session.dto.MeetingProgressSummary;
import com.learnkk.session.dto.MenteeCompletionResponse;
import com.learnkk.session.service.CompletionService;
import com.learnkk.session.service.SessionService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * U9 Admin/Monitoring — 소유 데이터 없는 read/조회 계층(ADR-007). 관리자 대시보드용 승인 큐 집계(US-9.1)와 운영
 * 현황 모니터링(US-9.2)을 U3/U4/U5 read 조합으로 구성한다. 모든 조회는 관리자 전용(BR-U9-1); 실제 승인 액션은 소유
 * Unit(U3 {@code MeetingApprovalService}, U5 {@code CompletionService})이 수행한다.
 *
 * <p>부분 실패 완화(BR-U9-5, graceful): 모니터링은 모임 행별로, 승인 큐는 큐별로 read 를 격리해, 한 소스 read 실패가 전체
 * 대시보드를 무너뜨리지 않도록 한다.
 */
@Service
public class AdminQueryService {

  private final MeetingService meetingService;
  private final EnrollmentService enrollmentService;
  private final CompletionService completionService;
  private final SessionService sessionService;

  public AdminQueryService(
      MeetingService meetingService,
      EnrollmentService enrollmentService,
      CompletionService completionService,
      SessionService sessionService) {
    this.meetingService = meetingService;
    this.enrollmentService = enrollmentService;
    this.completionService = completionService;
    this.sessionService = sessionService;
  }

  /** US-9.2 운영 현황 모니터링: 모임별 상태·신청/정원·세션 기준 출석율·수료 진행. */
  @Transactional(readOnly = true)
  public List<MeetingMonitorRow> getMonitoring(Principal principal) {
    requireAdmin(principal);
    List<MeetingMonitorRow> rows = new ArrayList<>();
    for (MeetingResponse m : meetingService.listAllMeetings()) {
      int applicants = safeApplicantCount(m.id());
      MeetingProgressSummary progress = safeProgress(m.id());
      rows.add(
          new MeetingMonitorRow(
              m.id(),
              m.title(),
              m.mentorId(),
              m.status(),
              m.capacity(),
              applicants,
              progress.participantCount(),
              progress.attendanceRatePercent(),
              progress.completionCandidates(),
              progress.completedCount()));
    }
    return rows;
  }

  /** US-9.1 승인 큐 집계: 5개 큐(①개설·모집확정·②시작·③모임완료·④멘티수료) 표시용 조회. */
  @Transactional(readOnly = true)
  public ApprovalQueues getApprovalQueues(Principal principal) {
    requireAdmin(principal);
    OffsetDateTime now = OffsetDateTime.now();

    List<MeetingQueueItem> creation =
        meetingService.listByStatus(MeetingStatus.PENDING_APPROVAL).stream()
            .map(MeetingQueueItem::from)
            .toList();

    // 모집 확정 대기: RECRUITING 이면서 모집기간이 종료된 모임(모집종료일이 지정되어 지난 경우) [assumption].
    List<MeetingQueueItem> recruitConfirm =
        meetingService.listByStatus(MeetingStatus.RECRUITING).stream()
            .filter(m -> m.recruitEnd() != null && !m.recruitEnd().isAfter(now))
            .map(MeetingQueueItem::from)
            .toList();

    List<MeetingQueueItem> start =
        meetingService.listByStatus(MeetingStatus.READY_TO_START).stream()
            .map(MeetingQueueItem::from)
            .toList();

    // 모임 완료 대기: IN_PROGRESS 이면서 U5 전 세션 종료.
    List<MeetingQueueItem> meetingComplete =
        meetingService.listByStatus(MeetingStatus.IN_PROGRESS).stream()
            .filter(m -> sessionService.allScheduledSessionsEnded(m.id()))
            .map(MeetingQueueItem::from)
            .toList();

    // 멘티 수료 대기: U5 COMPLETION_CANDIDATE. 모임 제목은 U3 read 로 보강.
    List<MenteeCompletionQueueItem> menteeComplete = new ArrayList<>();
    for (MenteeCompletionResponse c : completionService.listCompletionCandidates()) {
      menteeComplete.add(
          new MenteeCompletionQueueItem(
              c.meetingId(),
              safeMeetingTitle(c.meetingId()),
              c.menteeId(),
              c.attendedCount(),
              c.totalScheduled()));
    }

    return new ApprovalQueues(creation, recruitConfirm, start, meetingComplete, menteeComplete);
  }

  private int safeApplicantCount(Long meetingId) {
    try {
      return enrollmentService.countActiveApplicants(meetingId);
    } catch (RuntimeException e) {
      return 0;
    }
  }

  private MeetingProgressSummary safeProgress(Long meetingId) {
    try {
      return completionService.getMeetingProgress(meetingId);
    } catch (RuntimeException e) {
      return new MeetingProgressSummary(0, 0, 0, 0, 0);
    }
  }

  private String safeMeetingTitle(Long meetingId) {
    try {
      return meetingService.getMeeting(meetingId).title();
    } catch (RuntimeException e) {
      return null;
    }
  }

  private void requireAdmin(Principal principal) {
    if (!principal.isAdmin()) {
      throw new ForbiddenException(ErrorCodes.MEETING_FORBIDDEN, "관리자만 조회할 수 있습니다.");
    }
  }
}
