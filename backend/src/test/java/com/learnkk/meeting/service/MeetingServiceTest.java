package com.learnkk.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.dto.MeetingCreateRequest;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.MeetingSummary;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock private MeetingRepository meetingRepository;

  @InjectMocks private MeetingService meetingService;

  private final Principal mentor = new Principal(1L, Role.MENTOR);
  private final Principal mentee = new Principal(2L, Role.MENTEE);

  private MeetingCreateRequest validRequest() {
    return new MeetingCreateRequest(
        "Spring 스터디",
        "backend",
        8,
        OffsetDateTime.now(),
        OffsetDateTime.now().plusDays(7),
        5,
        "online",
        "intro");
  }

  @Test
  void createMeeting_happyPath_pendingApproval() {
    when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));

    MeetingResponse response = meetingService.createMeeting(mentor, validRequest());

    assertThat(response.status()).isEqualTo(MeetingStatus.PENDING_APPROVAL);
    assertThat(response.title()).isEqualTo("Spring 스터디");
    assertThat(response.mentorId()).isEqualTo(1L);
  }

  @Test
  void createMeeting_nonMentor_forbidden403() {
    assertThatThrownBy(() -> meetingService.createMeeting(mentee, validRequest()))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
    verify(meetingRepository, never()).save(any());
  }

  @Test
  void createMeeting_invalidWeeks_validation400() {
    MeetingCreateRequest bad = new MeetingCreateRequest("t", null, 0, null, null, 5, null, null);

    assertThatThrownBy(() -> meetingService.createMeeting(mentor, bad))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_VALIDATION);
  }

  @Test
  void createMeeting_invalidCapacity_validation400() {
    MeetingCreateRequest bad = new MeetingCreateRequest("t", null, 4, null, null, 0, null, null);

    assertThatThrownBy(() -> meetingService.createMeeting(mentor, bad))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_VALIDATION);
  }

  @Test
  void createMeeting_recruitEndBeforeStart_validation400() {
    MeetingCreateRequest bad =
        new MeetingCreateRequest(
            "t", null, 4, OffsetDateTime.now(), OffsetDateTime.now().minusDays(1), 5, null, null);

    assertThatThrownBy(() -> meetingService.createMeeting(mentor, bad))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_VALIDATION);
  }

  @Test
  void listRecruiting_filtersByRecruitingStatus() {
    Meeting m = new Meeting(1L, "t", null, 4, null, null, 5, null, null);
    m.setStatus(MeetingStatus.RECRUITING);
    Pageable pageable = PageRequest.of(0, 20);
    when(meetingRepository.findByStatus(MeetingStatus.RECRUITING, pageable))
        .thenReturn(new PageImpl<>(List.of(m), pageable, 1));

    PageResponse<MeetingSummary> page = meetingService.listRecruiting(pageable);

    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.content()).hasSize(1);
    assertThat(page.content().get(0).status()).isEqualTo(MeetingStatus.RECRUITING);
  }

  @Test
  void listMyMeetings_asMentor_returnsOwnMeetings() {
    Meeting m1 = new Meeting(1L, "내 모임", null, 4, null, null, 5, null, null);
    m1.setStatus(MeetingStatus.READY_TO_START);
    Pageable pageable = PageRequest.of(0, 20);
    when(meetingRepository.findByMentorId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(m1), pageable, 1));

    PageResponse<MeetingSummary> page = meetingService.listMyMeetings(mentor, pageable);

    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.content().get(0).title()).isEqualTo("내 모임");
    assertThat(page.content().get(0).status()).isEqualTo(MeetingStatus.READY_TO_START);
  }

  @Test
  void listMyMeetings_nonMentor_forbidden403() {
    Pageable pageable = PageRequest.of(0, 20);

    assertThatThrownBy(() -> meetingService.listMyMeetings(mentee, pageable))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
  }
}
