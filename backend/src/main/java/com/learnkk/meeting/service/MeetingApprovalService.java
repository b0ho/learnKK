package com.learnkk.meeting.service;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.MeetingSummary;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-only meeting lifecycle transitions (ADR-006 state machine owner). Bolt 2 completes the
 * lifecycle on top of the Bolt 1 creation gate:
 *
 * <ul>
 *   <li>T1 {@link #approveCreation} — PENDING_APPROVAL -&gt; RECRUITING
 *   <li>T2 {@link #rejectCreation} — PENDING_APPROVAL -&gt; REJECTED
 *   <li>T3/T4 {@link #confirmRecruitment} — RECRUITING -&gt; READY_TO_START | CANCELLED
 *   <li>T5 {@link #approveStart} — READY_TO_START -&gt; IN_PROGRESS
 *   <li>T6 {@link #completeMeeting} — IN_PROGRESS -&gt; COMPLETED (세션 종료와 무관, FR-6)
 * </ul>
 *
 * <p>Every transition reuses the conditional-UPDATE primitive {@code transitionStatus}; a 0-row
 * result means the meeting was not in the expected {@code from} state (illegal transition or a lost
 * race) and always maps to 409 {@code MEETING_INVALID_TRANSITION}.
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

  /**
   * T3/T4: confirm recruitment. When {@code proceed} is true the meeting advances RECRUITING -&gt;
   * READY_TO_START (T3); when false it is cancelled RECRUITING -&gt; CANCELLED (T4) and the
   * mandatory {@code reason} is persisted (reusing the reject_reason column). The proceed/cancel
   * decision is the admin's input alone — applicant counts are a screen aid only (U4 backend read
   * is not built in Bolt 2).
   */
  @Transactional
  public MeetingResponse confirmRecruitment(
      Principal principal, Long meetingId, boolean proceed, String reason) {
    requireAdmin(principal);
    ensureExists(meetingId);
    MeetingStatus target = proceed ? MeetingStatus.READY_TO_START : MeetingStatus.CANCELLED;
    String rejectReason = null;
    if (!proceed) {
      if (reason == null || reason.isBlank()) {
        throw new ValidationException(ErrorCodes.MEETING_VALIDATION, "취소 사유는 필수입니다.");
      }
      rejectReason = reason;
    }
    int updated =
        meetingRepository.transitionStatus(
            meetingId, MeetingStatus.RECRUITING, target, rejectReason);
    if (updated == 0) {
      throw new ConflictException(
          ErrorCodes.MEETING_INVALID_TRANSITION, "모집 확정을 할 수 없는 상태입니다. 이미 처리되었을 수 있습니다.");
    }
    return MeetingResponse.from(reload(meetingId));
  }

  /** T5: approve start. READY_TO_START -&gt; IN_PROGRESS. */
  @Transactional
  public MeetingResponse approveStart(Principal principal, Long meetingId) {
    requireAdmin(principal);
    ensureExists(meetingId);
    int updated =
        meetingRepository.transitionStatus(
            meetingId, MeetingStatus.READY_TO_START, MeetingStatus.IN_PROGRESS, null);
    if (updated == 0) {
      throw new ConflictException(
          ErrorCodes.MEETING_INVALID_TRANSITION, "시작할 수 없는 상태입니다. 이미 처리되었을 수 있습니다.");
    }
    return MeetingResponse.from(reload(meetingId));
  }

  /**
   * T6: complete the meeting. IN_PROGRESS -&gt; COMPLETED. 세션 종료 여부와 무관하게 관리자가 완료 처리할 수 있다
   * (ux-bugfixes-2 FR-6): 세션 종료 게이트를 제거하여 진행 중 어느 시점에도 완료가 가능하다. 상태 전이만 검증하며,
   * 비 IN_PROGRESS 에서의 완료는 조건부 UPDATE 0-row → 409 {@code MEETING_INVALID_TRANSITION}.
   */
  @Transactional
  public MeetingResponse completeMeeting(Principal principal, Long meetingId) {
    requireAdmin(principal);
    ensureExists(meetingId);
    int updated =
        meetingRepository.transitionStatus(
            meetingId, MeetingStatus.IN_PROGRESS, MeetingStatus.COMPLETED, null);
    if (updated == 0) {
      throw new ConflictException(
          ErrorCodes.MEETING_INVALID_TRANSITION, "완료할 수 없는 상태입니다. 이미 처리되었을 수 있습니다.");
    }
    return MeetingResponse.from(reload(meetingId));
  }

  /**
   * 승인 되돌리기(FR-5): 전진 승인을 직전 상태로 원자적 역전이한다(관리자 전용). RECRUITING→PENDING_APPROVAL,
   * READY_TO_START→RECRUITING, IN_PROGRESS→READY_TO_START, COMPLETED→IN_PROGRESS. 반려(REJECTED)·모집취소
   * (CANCELLED)·최초 상태(PENDING_APPROVAL)는 되돌리기 대상이 아니며 409 {@code MEETING_INVALID_TRANSITION}.
   */
  @Transactional
  public MeetingResponse revert(Principal principal, Long meetingId) {
    requireAdmin(principal);
    Meeting meeting = reload(meetingId);
    MeetingStatus from = meeting.getStatus();
    MeetingStatus to = priorStatus(from);
    if (to == null) {
      throw new ConflictException(
          ErrorCodes.MEETING_INVALID_TRANSITION, "되돌릴 수 없는 상태입니다.");
    }
    int updated = meetingRepository.transitionStatus(meetingId, from, to, null);
    if (updated == 0) {
      throw new ConflictException(
          ErrorCodes.MEETING_INVALID_TRANSITION, "되돌릴 수 없는 상태입니다. 이미 처리되었을 수 있습니다.");
    }
    return MeetingResponse.from(reload(meetingId));
  }

  private MeetingStatus priorStatus(MeetingStatus current) {
    return switch (current) {
      case RECRUITING -> MeetingStatus.PENDING_APPROVAL;
      case READY_TO_START -> MeetingStatus.RECRUITING;
      case IN_PROGRESS -> MeetingStatus.READY_TO_START;
      case COMPLETED -> MeetingStatus.IN_PROGRESS;
      default -> null; // PENDING_APPROVAL / REJECTED / CANCELLED — 되돌리기 대상 아님
    };
  }

  /** 관리자 승인 큐(FR-2/FR-3): 특정 상태의 모임 목록. 관리자 전용. */
  @Transactional(readOnly = true)
  public PageResponse<MeetingSummary> listByStatus(
      Principal principal, MeetingStatus status, Pageable pageable) {
    requireAdmin(principal);
    return PageResponse.from(
        meetingRepository.findByStatus(status, pageable).map(MeetingSummary::from));
  }

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
