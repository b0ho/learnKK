package com.learnkk.admin.service;

import com.learnkk.admin.dto.MeetingMonitoringSummary;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.CompletionStatus;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import com.learnkk.session.entity.MeetingSession;
import com.learnkk.session.entity.MenteeCompletion;
import com.learnkk.session.repository.AttendanceRepository;
import com.learnkk.session.repository.MeetingSessionRepository;
import com.learnkk.session.repository.MenteeCompletionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * U9 Admin/Monitoring(C8) — 운영 현황 모니터링 read 조합 계층(US-9.2, ADR-007). 승인 액션은 U3/U5 Service 가
 * 소유하고, 여기는 모임(U3)·신청(U4)·세션/출석/수료(U5) 의 read 만 조합해 모임별 상태·출석율(세션 기준)·수료 진행을
 * 집계한다. 집계 지표(개설 대비 승인률 등)는 범위 밖(US-9.3 Won't).
 */
@Service
public class AdminMonitoringService {

  private final MeetingRepository meetingRepository;
  private final MeetingSessionRepository sessionRepository;
  private final AttendanceRepository attendanceRepository;
  private final MenteeCompletionRepository completionRepository;
  private final EnrollmentService enrollmentService;
  private final UserRepository userRepository;

  public AdminMonitoringService(
      MeetingRepository meetingRepository,
      MeetingSessionRepository sessionRepository,
      AttendanceRepository attendanceRepository,
      MenteeCompletionRepository completionRepository,
      EnrollmentService enrollmentService,
      UserRepository userRepository) {
    this.meetingRepository = meetingRepository;
    this.sessionRepository = sessionRepository;
    this.attendanceRepository = attendanceRepository;
    this.completionRepository = completionRepository;
    this.enrollmentService = enrollmentService;
    this.userRepository = userRepository;
  }

  /**
   * 운영 현황 모니터링 목록(US-9.2). 관리자 전용(403). status 를 주면 해당 상태만, 없으면 전체 모임을 페이지로
   * 조회하고 모임별로 세션·출석·수료 read 를 조합한다. 페이지 크기가 [1,100] 로 클램프되므로 행당 소수 read 는
   * 허용 범위(M 복잡도, 조회 전용 화면).
   */
  @Transactional(readOnly = true)
  public PageResponse<MeetingMonitoringSummary> listMeetings(
      Principal principal, MeetingStatus status, Pageable pageable) {
    requireAdmin(principal);
    Page<Meeting> meetings =
        status == null
            ? meetingRepository.findAll(pageable)
            : meetingRepository.findByStatus(status, pageable);
    OffsetDateTime now = OffsetDateTime.now();
    return PageResponse.from(meetings.map(m -> toSummary(m, now)));
  }

  private MeetingMonitoringSummary toSummary(Meeting meeting, OffsetDateTime now) {
    Long meetingId = meeting.getId();

    List<MeetingSession> sessions =
        sessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc(meetingId);
    int sessionCount = sessions.size();
    int endedSessionCount = (int) sessions.stream().filter(s -> s.isEnded(now)).count();

    int menteeCount = enrollmentService.listActiveMenteeIds(meetingId).size();

    // 출석율(세션 기준): 멘티별 a/S(BR-U5-3)의 모임 평균 = 총 출석 수 / (S × 멘티 수). 분모 0 이면 0.0.
    long attendedTotal = attendanceRepository.countByMeetingId(meetingId);
    long denominator = (long) sessionCount * menteeCount;
    double attendanceRate = denominator > 0 ? (double) attendedTotal / denominator : 0.0;

    List<MenteeCompletion> completions = completionRepository.findByMeetingId(meetingId);
    int completed =
        (int) completions.stream().filter(c -> c.getStatus() == CompletionStatus.COMPLETED).count();
    int candidates =
        (int)
            completions.stream()
                .filter(c -> c.getStatus() == CompletionStatus.COMPLETION_CANDIDATE)
                .count();

    // 멘토 닉네임은 최소 U2 read (U9→U2 허용, EnrollmentService.resolveNickname 과 동일 패턴).
    String mentorNickname =
        userRepository.findById(meeting.getMentorId()).map(User::getNickname).orElse(null);

    return new MeetingMonitoringSummary(
        meetingId,
        meeting.getTitle(),
        meeting.getStatus(),
        meeting.getMentorId(),
        mentorNickname,
        menteeCount,
        sessionCount,
        endedSessionCount,
        attendanceRate,
        completed,
        candidates,
        meeting.getMentorCompletionStatus());
  }

  private void requireAdmin(Principal principal) {
    if (!principal.isAdmin()) {
      throw new ForbiddenException(ErrorCodes.MONITORING_FORBIDDEN, "관리자만 운영 현황을 조회할 수 있습니다.");
    }
  }
}
