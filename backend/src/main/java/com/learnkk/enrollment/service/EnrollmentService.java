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
import java.util.Collection;
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

    // Friendly pre-check before the atomic section. A CANCELLED row is REUSED (FR-12 재신청):
    // only an already-APPLIED row is a true duplicate.
    var existing = enrollmentRepository.findByMeetingIdAndMenteeId(meetingId, principal.userId());
    if (existing.isPresent() && existing.get().getStatus() == EnrollmentStatus.APPLIED) {
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
      // 취소했던 신청이 있으면 재활성화, 없으면 신규 생성(FR-12).
      Enrollment enrollment = existing.orElseGet(() -> new Enrollment(meetingId, principal.userId()));
      if (existing.isPresent()) {
        enrollment.reactivate();
      }
      Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
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

  /**
   * Cross-module participant check (U5 attendance/completion + U8 survey/feedback read-in): true
   * when the mentee holds an APPLIED enrollment for the meeting. Callers gate their own actions on
   * actual participants without touching the enrollment table directly (ADR-007 R-1).
   */
  @Transactional(readOnly = true)
  public boolean isActiveParticipant(Long meetingId, Long menteeId) {
    return enrollmentRepository.existsByMeetingIdAndMenteeIdAndStatus(
        meetingId, menteeId, EnrollmentStatus.APPLIED);
  }

  /** List the caller's own enrollments (US-3.5). Meeting details are composed on the FE. */
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> listMyEnrollments(Principal principal) {
    return enrollmentRepository.findByMenteeIdOrderByAppliedAtDesc(principal.userId()).stream()
        .map(EnrollmentResponse::from)
        .toList();
  }

  /**
   * Read port for cross-unit participant authorization (U6 content). Returns whether {@code userId}
   * holds an APPLIED enrollment for {@code meetingId} — i.e. is a participating mentee. Callers
   * reach this via the {@link EnrollmentService} interface only (no direct table access); the U6→U4
   * read edge is acyclic since U4 never reads U6.
   */
  @Transactional(readOnly = true)
  public boolean isParticipant(Long meetingId, Long userId) {
    return enrollmentRepository
        .findByMeetingIdAndMenteeId(meetingId, userId)
        .filter(e -> e.getStatus() == EnrollmentStatus.APPLIED)
        .isPresent();
  }

  // --- Cross-module reads (U7 messaging authorization; ADR-007 — no cross-table joins) ---

  /** Whether the mentee holds an active (APPLIED) enrollment in any of the given meetings. */
  @Transactional(readOnly = true)
  public boolean isActivelyEnrolledInAnyOf(Collection<Long> meetingIds, Long menteeId) {
    if (meetingIds.isEmpty()) {
      return false;
    }
    return enrollmentRepository.existsByMeetingIdInAndMenteeIdAndStatus(
        meetingIds, menteeId, EnrollmentStatus.APPLIED);
  }

  /** Meeting ids in which the mentee currently holds an active enrollment. */
  @Transactional(readOnly = true)
  public List<Long> activeMeetingIdsForMentee(Long menteeId) {
    return enrollmentRepository.findMeetingIdsByMenteeIdAndStatus(
        menteeId, EnrollmentStatus.APPLIED);
  }

  /** Distinct mentee ids actively enrolled in any of the given meetings. */
  @Transactional(readOnly = true)
  public List<Long> activeMenteeIdsForMeetings(Collection<Long> meetingIds) {
    if (meetingIds.isEmpty()) {
      return List.of();
    }
    return enrollmentRepository.findMenteeIdsByMeetingIdInAndStatus(
        meetingIds, EnrollmentStatus.APPLIED);
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
   * 무권한 cross-module read 포트(U9→U4, ADR-007 R-1): 해당 모임의 활성(APPLIED) 신청 수를 반환한다. 관리자 운영
   * 현황 모니터링의 신청 수 집계로 사용된다. 인가는 호출 측(U9)이 담당한다.
   */
  @Transactional(readOnly = true)
  public int countActiveApplicants(Long meetingId) {
    return enrollmentRepository.countByMeetingIdAndStatus(meetingId, EnrollmentStatus.APPLIED);
  }

  private String resolveNickname(Long menteeId) {
    return userRepository.findById(menteeId).map(User::getNickname).orElse(null);
  }
}
