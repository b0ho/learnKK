package com.learnkk.meeting.service;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-only meeting lifecycle transitions. Bolt 1 implements only the creation gate: T1 approve
 * (PENDING_APPROVAL -&gt; RECRUITING) and T2 reject (PENDING_APPROVAL -&gt; REJECTED).
 */
@Service
public class MeetingApprovalService {

  private final MeetingRepository meetingRepository;

  public MeetingApprovalService(MeetingRepository meetingRepository) {
    this.meetingRepository = meetingRepository;
  }

  /**
   * T1: approve creation. Conditional UPDATE guards against illegal transitions / double-approve.
   */
  @Transactional
  public MeetingResponse approveCreation(Principal principal, Long meetingId) {
    requireAdmin(principal);
    ensureExists(meetingId);
    int updated =
        meetingRepository.transitionStatus(
            meetingId, MeetingStatus.PENDING_APPROVAL, MeetingStatus.RECRUITING, null);
    if (updated == 0) {
      throw new ConflictException(
          ErrorCodes.MEETING_INVALID_TRANSITION, "승인할 수 없는 상태입니다. 이미 처리되었을 수 있습니다.");
    }
    return MeetingResponse.from(reload(meetingId));
  }

  /** T2: reject creation, persisting the reject reason. */
  @Transactional
  public MeetingResponse rejectCreation(Principal principal, Long meetingId, String reason) {
    requireAdmin(principal);
    ensureExists(meetingId);
    int updated =
        meetingRepository.transitionStatus(
            meetingId, MeetingStatus.PENDING_APPROVAL, MeetingStatus.REJECTED, reason);
    if (updated == 0) {
      throw new ConflictException(
          ErrorCodes.MEETING_INVALID_TRANSITION, "반려할 수 없는 상태입니다. 이미 처리되었을 수 있습니다.");
    }
    return MeetingResponse.from(reload(meetingId));
  }

  // Bolt 2+: T3-T6 (confirmRecruitment / approveStart / completeMeeting) — not implemented.

  private void requireAdmin(Principal principal) {
    if (!principal.isAdmin()) {
      throw new ForbiddenException(ErrorCodes.MEETING_FORBIDDEN, "관리자만 수행할 수 있습니다.");
    }
  }

  private void ensureExists(Long meetingId) {
    if (!meetingRepository.existsById(meetingId)) {
      throw new NotFoundException(ErrorCodes.MEETING_NOT_FOUND, "모임을 찾을 수 없습니다.");
    }
  }

  private Meeting reload(Long meetingId) {
    return meetingRepository
        .findById(meetingId)
        .orElseThrow(() -> new NotFoundException(ErrorCodes.MEETING_NOT_FOUND, "모임을 찾을 수 없습니다."));
  }
}
