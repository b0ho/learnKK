package com.learnkk.enrollment.service;

import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.enrollment.domain.EnrollmentStatus;
import com.learnkk.enrollment.dto.ApplicantResponse;
import com.learnkk.enrollment.dto.EnrollmentResponse;
import com.learnkk.enrollment.entity.Enrollment;
import com.learnkk.enrollment.repository.EnrollmentRepository;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enrollment workflow (C3, U4): first-come-first-served application with capacity/duplicate
 * control, cancellation, applicant listing and the mentee's own enrollment listing. Meeting
 * capacity and status are read from U3 via {@link MeetingService} (ADR-007 R-1); this service never
 * writes the meetings table.
 */
@Service
public class EnrollmentService {

  /** Meeting states in which a mentee may still cancel — i.e. before start ② (BR-U4-3). */
  private static final Set<MeetingStatus> CANCELLABLE =
      EnumSet.of(MeetingStatus.RECRUITING, MeetingStatus.READY_TO_START);

  private final EnrollmentRepository enrollmentRepository;
  private final MeetingService meetingService;
  private final UserRepository userRepository;

  public EnrollmentService(
      EnrollmentRepository enrollmentRepository,
      MeetingService meetingService,
      UserRepository userRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.meetingService = meetingService;
    this.userRepository = userRepository;
  }

  /**
   * Apply to a meeting (US-3.2). MENTEE-only. The meeting must be RECRUITING. Capacity is enforced
   * race-free under a per-meeting advisory lock (BR-U4-1); the UNIQUE(meeting_id, mentee_id)
   * constraint backstops concurrent duplicate applications (BR-U4-2).
   */
  @Transactional
  public EnrollmentResponse apply(Principal principal, Long meetingId) {
    if (!principal.isMentee()) {
      throw new ForbiddenException(ErrorCodes.ENROLLMENT_FORBIDDEN, "멘티만 모임에 신청할 수 있습니다.");
    }
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    if (meeting.status() != MeetingStatus.RECRUITING) {
      throw new ConflictException(ErrorCodes.ENROLLMENT_NOT_OPEN, "모집 중인 모임이 아닙니다.");
    }

    // Friendly pre-check before entering the atomic section (a CANCELLED row still occupies the
    // (meeting, mentee) pair — re-application is not supported).
    if (enrollmentRepository
        .findByMeetingIdAndMenteeId(meetingId, principal.userId())
        .isPresent()) {
      throw new ConflictException(ErrorCodes.ENROLLMENT_DUPLICATE, "이미 신청한 모임입니다.");
    }

    // --- Atomic section (BR-U4-1): serialize per meeting so count-then-insert is race-free. ---
    enrollmentRepository.lockMeeting(meetingId);
    int active =
        enrollmentRepository.countByMeetingIdAndStatus(meetingId, EnrollmentStatus.APPLIED);
    if (active >= meeting.capacity()) {
      throw new ConflictException(ErrorCodes.ENROLLMENT_FULL, "모집 정원이 마감되었습니다.");
    }

    try {
      Enrollment saved =
          enrollmentRepository.saveAndFlush(new Enrollment(meetingId, principal.userId()));
      return EnrollmentResponse.from(saved);
    } catch (DataIntegrityViolationException e) {
      // Concurrent duplicate lost the UNIQUE race.
      throw new ConflictException(ErrorCodes.ENROLLMENT_DUPLICATE, "이미 신청한 모임입니다.");
    }
  }

  /**
   * Cancel the caller's own application (US-3.3). Allowed only before start ② (meeting in
   * {RECRUITING, READY_TO_START}); the freed seat returns to capacity.
   */
  @Transactional
  public void cancel(Principal principal, Long meetingId) {
    Enrollment enrollment =
        enrollmentRepository
            .findByMeetingIdAndMenteeId(meetingId, principal.userId())
            .filter(e -> e.getStatus() == EnrollmentStatus.APPLIED)
            .orElseThrow(
                () -> new NotFoundException(ErrorCodes.ENROLLMENT_NOT_FOUND, "신청 내역을 찾을 수 없습니다."));

    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    if (!CANCELLABLE.contains(meeting.status())) {
      throw new ConflictException(
          ErrorCodes.ENROLLMENT_CANCEL_FORBIDDEN, "모임이 시작된 이후에는 신청을 취소할 수 없습니다.");
    }

    enrollment.cancel();
    enrollmentRepository.save(enrollment);
  }

  /**
   * List the APPLIED applicants of a meeting with their mentee nicknames (US-2.3). Restricted to
   * the owning mentor or an admin.
   */
  @Transactional(readOnly = true)
  public List<ApplicantResponse> listApplicants(Principal principal, Long meetingId) {
    MeetingResponse meeting = meetingService.getMeeting(meetingId);
    boolean owningMentor = principal.isMentor() && meeting.mentorId().equals(principal.userId());
    if (!owningMentor && !principal.isAdmin()) {
      throw new ForbiddenException(ErrorCodes.ENROLLMENT_FORBIDDEN, "신청자 목록을 조회할 권한이 없습니다.");
    }

    return enrollmentRepository
        .findByMeetingIdAndStatus(meetingId, EnrollmentStatus.APPLIED)
        .stream()
        .map(
            e ->
                new ApplicantResponse(
                    e.getMenteeId(), resolveNickname(e.getMenteeId()), e.getAppliedAt()))
        .toList();
  }

  /** List the caller's own enrollments (US-3.5). Meeting details are composed on the FE. */
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> listMyEnrollments(Principal principal) {
    return enrollmentRepository.findByMenteeIdOrderByAppliedAtDesc(principal.userId()).stream()
        .map(EnrollmentResponse::from)
        .toList();
  }

  /**
   * 무권한 cross-module read 포트(U5→U4, ADR-007 R-1): 해당 모임의 참여 멘티(APPLIED) id 목록을 반환한다.
   * 수료 판정 대상 집합(computeCompletion)으로 사용된다. 인가는 호출 측(U5)이 담당한다.
   */
  @Transactional(readOnly = true)
  public List<Long> listActiveMenteeIds(Long meetingId) {
    return enrollmentRepository
        .findByMeetingIdAndStatus(meetingId, EnrollmentStatus.APPLIED)
        .stream()
        .map(Enrollment::getMenteeId)
        .toList();
  }

  /**
   * 무권한 cross-module read 포트(U5→U4): 멘티가 해당 모임의 활성 참여자(APPLIED)인지 여부. 출석 참여자 게이트로
   * 사용된다.
   */
  @Transactional(readOnly = true)
  public boolean isActiveParticipant(Long meetingId, Long menteeId) {
    return enrollmentRepository.existsByMeetingIdAndMenteeIdAndStatus(
        meetingId, menteeId, EnrollmentStatus.APPLIED);
  }

  private String resolveNickname(Long menteeId) {
    return userRepository.findById(menteeId).map(User::getNickname).orElse(null);
  }
}
