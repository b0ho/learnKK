package com.learnkk.meeting.service;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.dto.MeetingCreateRequest;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.MeetingSummary;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Meeting creation, lookup and the recruiting listing. */
@Service
public class MeetingService {

  private final MeetingRepository meetingRepository;

  public MeetingService(MeetingRepository meetingRepository) {
    this.meetingRepository = meetingRepository;
  }

  @Transactional
  public MeetingResponse createMeeting(Principal principal, MeetingCreateRequest request) {
    if (!principal.isMentor()) {
      throw new ForbiddenException(ErrorCodes.MEETING_FORBIDDEN, "멘토만 모임을 개설할 수 있습니다.");
    }
    validate(request);

    Meeting meeting =
        new Meeting(
            principal.userId(),
            request.title().trim(),
            request.topic(),
            request.weeks(),
            request.recruitStart(),
            request.recruitEnd(),
            request.capacity(),
            request.format(),
            request.initialContent());
    // New meetings always start in PENDING_APPROVAL awaiting admin approval (T1).
    meeting.setStatus(MeetingStatus.PENDING_APPROVAL);
    return MeetingResponse.from(meetingRepository.save(meeting));
  }

  @Transactional(readOnly = true)
  public MeetingResponse getMeeting(Long id) {
    return MeetingResponse.from(loadMeeting(id));
  }

  /**
   * Ids of every meeting owned by the given mentor. Cross-module read used by messaging (U7) to
   * authorize a mentor messaging a mentee enrolled in one of the mentor's meetings (ADR-007).
   */
  @Transactional(readOnly = true)
  public List<Long> meetingIdsOwnedBy(Long mentorId) {
    return meetingRepository.findMeetingIdsByMentorId(mentorId);
  }

  /**
   * Distinct owners (mentor ids) of the given meetings. Cross-module read used by messaging (U7) to
   * authorize a mentee messaging the mentor of a meeting they applied to (ADR-007).
   */
  @Transactional(readOnly = true)
  public List<Long> mentorIdsForMeetings(Collection<Long> meetingIds) {
    if (meetingIds.isEmpty()) {
      return List.of();
    }
    return meetingRepository.findMentorIdsByIdIn(meetingIds);
  }

  @Transactional(readOnly = true)
  public PageResponse<MeetingSummary> listRecruiting(Pageable pageable) {
    return PageResponse.from(
        meetingRepository
            .findByStatus(MeetingStatus.RECRUITING, pageable)
            .map(MeetingSummary::from));
  }

  /**
   * Mentor operations hub: lists the caller's own meetings (any status) so the mentor can see each
   * meeting's status and next action. Only mentors may call this (BR-U3-6, US-2.3).
   */
  @Transactional(readOnly = true)
  public PageResponse<MeetingSummary> listMyMeetings(Principal principal, Pageable pageable) {
    if (!principal.isMentor()) {
      throw new ForbiddenException(ErrorCodes.MEETING_FORBIDDEN, "멘토만 자신의 모임 목록을 조회할 수 있습니다.");
    }
    return PageResponse.from(
        meetingRepository.findByMentorId(principal.userId(), pageable).map(MeetingSummary::from));
  }

  Meeting loadMeeting(Long id) {
    return meetingRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCodes.MEETING_NOT_FOUND, "모임을 찾을 수 없습니다."));
  }

  private void validate(MeetingCreateRequest request) {
    if (request.weeks() == null || request.weeks() <= 0) {
      throw new ValidationException(ErrorCodes.MEETING_VALIDATION, "진행 주차는 1 이상이어야 합니다.");
    }
    if (request.capacity() == null || request.capacity() <= 0) {
      throw new ValidationException(ErrorCodes.MEETING_VALIDATION, "모집 정원은 1 이상이어야 합니다.");
    }
    if (request.recruitStart() != null
        && request.recruitEnd() != null
        && !request.recruitEnd().isAfter(request.recruitStart())) {
      throw new ValidationException(ErrorCodes.MEETING_VALIDATION, "모집 종료일은 시작일보다 이후여야 합니다.");
    }
  }
}
