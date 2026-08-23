package com.learnkk.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.enrollment.domain.EnrollmentStatus;
import com.learnkk.enrollment.dto.ApplicantResponse;
import com.learnkk.enrollment.dto.EnrollmentResponse;
import com.learnkk.enrollment.entity.Enrollment;
import com.learnkk.enrollment.repository.EnrollmentRepository;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.service.MeetingService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private MeetingService meetingService;
  @Mock private UserRepository userRepository;

  @InjectMocks private EnrollmentService enrollmentService;

  private final Principal mentee = new Principal(2L, Role.MENTEE);
  private final Principal otherMentee = new Principal(3L, Role.MENTEE);
  private final Principal mentor = new Principal(1L, Role.MENTOR);
  private final Principal admin = new Principal(9L, Role.ADMIN);

  private MeetingResponse meeting(MeetingStatus status, int capacity) {
    return new MeetingResponse(
        10L, 1L, "Spring", "backend", 8, null, null, capacity, "online", "intro", status, null);
  }

  private Enrollment applied(Long id, Long meetingId, Long menteeId) {
    Enrollment e = new Enrollment(meetingId, menteeId);
    return e;
  }

  // --- apply ---

  @Test
  void apply_happyPath_returnsEnrollment() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));
    when(enrollmentRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(enrollmentRepository.countByMeetingIdAndStatus(10L, EnrollmentStatus.APPLIED))
        .thenReturn(2);
    when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    EnrollmentResponse response = enrollmentService.apply(mentee, 10L);

    assertThat(response.meetingId()).isEqualTo(10L);
    assertThat(response.menteeId()).isEqualTo(2L);
    assertThat(response.status()).isEqualTo(EnrollmentStatus.APPLIED);
    verify(enrollmentRepository).lockMeeting(10L);
  }

  @Test
  void apply_nonMentee_forbidden403() {
    assertThatThrownBy(() -> enrollmentService.apply(mentor, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_FORBIDDEN);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void apply_meetingNotRecruiting_conflict409NotOpen() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.READY_TO_START, 5));

    assertThatThrownBy(() -> enrollmentService.apply(mentee, 10L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_NOT_OPEN);
    verify(enrollmentRepository, never()).lockMeeting(anyLong());
  }

  @Test
  void apply_capacityFull_conflict409Full() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 3));
    when(enrollmentRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(enrollmentRepository.countByMeetingIdAndStatus(10L, EnrollmentStatus.APPLIED))
        .thenReturn(3);

    assertThatThrownBy(() -> enrollmentService.apply(mentee, 10L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_FULL);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void apply_alreadyApplied_conflict409Duplicate() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));
    when(enrollmentRepository.findByMeetingIdAndMenteeId(10L, 2L))
        .thenReturn(Optional.of(applied(1L, 10L, 2L)));

    assertThatThrownBy(() -> enrollmentService.apply(mentee, 10L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_DUPLICATE);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void apply_concurrentUniqueViolation_conflict409Duplicate() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));
    when(enrollmentRepository.findByMeetingIdAndMenteeId(10L, 2L)).thenReturn(Optional.empty());
    when(enrollmentRepository.countByMeetingIdAndStatus(10L, EnrollmentStatus.APPLIED))
        .thenReturn(0);
    when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .thenThrow(new DataIntegrityViolationException("uq_enrollment_meeting_mentee"));

    assertThatThrownBy(() -> enrollmentService.apply(mentee, 10L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_DUPLICATE);
  }

  // --- cancel ---

  @Test
  void cancel_happyPath_setsCancelled() {
    Enrollment enrollment = applied(1L, 10L, 2L);
    when(enrollmentRepository.findByMeetingIdAndMenteeId(10L, 2L))
        .thenReturn(Optional.of(enrollment));
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));

    enrollmentService.cancel(mentee, 10L);

    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(enrollment.getCancelledAt()).isNotNull();
    verify(enrollmentRepository).save(enrollment);
  }

  @Test
  void cancel_afterInProgress_conflict409CancelForbidden() {
    Enrollment enrollment = applied(1L, 10L, 2L);
    when(enrollmentRepository.findByMeetingIdAndMenteeId(10L, 2L))
        .thenReturn(Optional.of(enrollment));
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.IN_PROGRESS, 5));

    assertThatThrownBy(() -> enrollmentService.cancel(mentee, 10L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_CANCEL_FORBIDDEN);
    verify(enrollmentRepository, never()).save(any());
  }

  @Test
  void cancel_notOwner_notFound404() {
    when(enrollmentRepository.findByMeetingIdAndMenteeId(10L, 3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> enrollmentService.cancel(otherMentee, 10L))
        .isInstanceOf(NotFoundException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_NOT_FOUND);
  }

  // --- listApplicants ---

  @Test
  void listApplicants_owningMentor_returnsListWithNicknames() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));
    Enrollment e = applied(1L, 10L, 2L);
    when(enrollmentRepository.findByMeetingIdAndStatus(10L, EnrollmentStatus.APPLIED))
        .thenReturn(List.of(e));
    when(userRepository.findById(2L))
        .thenReturn(Optional.of(new User("멘티둘", "hash", "E-2", Role.MENTEE)));

    List<ApplicantResponse> applicants = enrollmentService.listApplicants(mentor, 10L);

    assertThat(applicants).hasSize(1);
    assertThat(applicants.get(0).menteeId()).isEqualTo(2L);
    assertThat(applicants.get(0).nickname()).isEqualTo("멘티둘");
  }

  @Test
  void listApplicants_asAdmin_allowed() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));
    when(enrollmentRepository.findByMeetingIdAndStatus(10L, EnrollmentStatus.APPLIED))
        .thenReturn(List.of());

    assertThat(enrollmentService.listApplicants(admin, 10L)).isEmpty();
  }

  @Test
  void listApplicants_nonOwnerMentor_forbidden403() {
    // Mentor 5 does not own meeting 10 (owner is mentor 1).
    Principal otherMentor = new Principal(5L, Role.MENTOR);
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));

    assertThatThrownBy(() -> enrollmentService.listApplicants(otherMentor, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_FORBIDDEN);
  }

  @Test
  void listApplicants_asMentee_forbidden403() {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING, 5));

    assertThatThrownBy(() -> enrollmentService.listApplicants(mentee, 10L))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ENROLLMENT_FORBIDDEN);
  }

  // --- listMyEnrollments ---

  @Test
  void listMyEnrollments_returnsOwnEnrollments() {
    Enrollment e = applied(1L, 10L, 2L);
    when(enrollmentRepository.findByMenteeIdOrderByAppliedAtDesc(2L)).thenReturn(List.of(e));

    List<EnrollmentResponse> mine = enrollmentService.listMyEnrollments(mentee);

    assertThat(mine).hasSize(1);
    assertThat(mine.get(0).meetingId()).isEqualTo(10L);
    assertThat(mine.get(0).status()).isEqualTo(EnrollmentStatus.APPLIED);
  }
}
