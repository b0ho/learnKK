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

  @Transactional(readOnly = true)
  public PageResponse<MeetingSummary> listRecruiting(Pageable pageable) {
    return PageResponse.from(
        meetingRepository
            .findByStatus(MeetingStatus.RECRUITING, pageable)
            .map(MeetingSummary::from));
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
