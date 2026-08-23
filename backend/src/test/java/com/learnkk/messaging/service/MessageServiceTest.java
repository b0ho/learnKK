package com.learnkk.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.messaging.dto.MessageResponse;
import com.learnkk.messaging.dto.RecipientResponse;
import com.learnkk.messaging.dto.ThreadSummaryResponse;
import com.learnkk.messaging.entity.Message;
import com.learnkk.messaging.entity.MessageThread;
import com.learnkk.messaging.repository.MessageRepository;
import com.learnkk.messaging.repository.MessageThreadRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock private MessageThreadRepository threadRepository;
  @Mock private MessageRepository messageRepository;
  @Mock private UserRepository userRepository;
  @Mock private MeetingService meetingService;
  @Mock private EnrollmentService enrollmentService;

  @InjectMocks private MessageService messageService;

  private final Principal mentor = new Principal(1L, Role.MENTOR);
  private final Principal mentee = new Principal(2L, Role.MENTEE);
  private final Principal admin = new Principal(9L, Role.ADMIN);

  private User user(long id, Role role) {
    User u = new User("user" + id, "hash", "E-" + id, role);
    ReflectionTestUtils.setField(u, "id", id);
    return u;
  }

  private void stubThreadCreateAndSend(long threadId) {
    MessageThread thread = mock(MessageThread.class);
    lenient().when(thread.getId()).thenReturn(threadId);
    lenient()
        .when(threadRepository.findByParticipantAAndParticipantB(any(), any()))
        .thenReturn(Optional.empty());
    lenient().when(threadRepository.saveAndFlush(any(MessageThread.class))).thenReturn(thread);
    lenient().when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
  }

  // --- send: validation ---

  @Test
  void send_emptyBody_validation400() {
    assertThatThrownBy(() -> messageService.send(mentee, 3L, "   "))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MESSAGING_EMPTY_BODY);
    verify(messageRepository, never()).save(any());
  }

  @Test
  void send_toSelf_validation400() {
    assertThatThrownBy(() -> messageService.send(mentee, 2L, "안녕"))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MESSAGING_SELF);
  }

  @Test
  void send_recipientNotFound_notFound404() {
    when(userRepository.findById(3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> messageService.send(mentee, 3L, "안녕"))
        .isInstanceOf(NotFoundException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MESSAGING_RECIPIENT_NOT_FOUND);
  }

  // --- send: permission boundary (the Bolt 5 DoD hypothesis) ---

  @Test
  void send_mentorToEnrolledMentee_succeeds() {
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.MENTEE)));
    when(meetingService.meetingIdsOwnedBy(1L)).thenReturn(List.of(10L));
    when(enrollmentService.isActivelyEnrolledInAnyOf(List.of(10L), 2L)).thenReturn(true);
    stubThreadCreateAndSend(100L);

    MessageResponse response = messageService.send(mentor, 2L, "안녕하세요");

    assertThat(response.body()).isEqualTo("안녕하세요");
    assertThat(response.senderId()).isEqualTo(1L);
    assertThat(response.threadId()).isEqualTo(100L);
  }

  @Test
  void send_mentorToUnrelatedMentee_forbidden403() {
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.MENTEE)));
    when(meetingService.meetingIdsOwnedBy(1L)).thenReturn(List.of(10L));
    when(enrollmentService.isActivelyEnrolledInAnyOf(List.of(10L), 2L)).thenReturn(false);

    assertThatThrownBy(() -> messageService.send(mentor, 2L, "안녕"))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MESSAGING_FORBIDDEN);
    verify(messageRepository, never()).save(any());
  }

  @Test
  void send_menteeToMentorOfAppliedMeeting_succeeds() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.MENTOR)));
    when(enrollmentService.activeMeetingIdsForMentee(2L)).thenReturn(List.of(10L));
    when(meetingService.mentorIdsForMeetings(List.of(10L))).thenReturn(List.of(1L));
    stubThreadCreateAndSend(100L);

    MessageResponse response = messageService.send(mentee, 1L, "질문 있습니다");

    assertThat(response.senderId()).isEqualTo(2L);
    assertThat(response.threadId()).isEqualTo(100L);
  }

  @Test
  void send_menteeToUnrelatedMentor_forbidden403() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.MENTOR)));
    when(enrollmentService.activeMeetingIdsForMentee(2L)).thenReturn(List.of(10L));
    when(meetingService.mentorIdsForMeetings(List.of(10L))).thenReturn(List.of(7L));

    assertThatThrownBy(() -> messageService.send(mentee, 1L, "안녕"))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MESSAGING_FORBIDDEN);
  }

  @Test
  void send_anyoneToAdmin_succeeds() {
    when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, Role.ADMIN)));
    stubThreadCreateAndSend(100L);

    MessageResponse response = messageService.send(mentee, 9L, "관리자님께");

    assertThat(response.senderId()).isEqualTo(2L);
  }

  @Test
  void send_adminToAnyone_succeeds() {
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.MENTEE)));
    stubThreadCreateAndSend(100L);

    MessageResponse response = messageService.send(admin, 2L, "공지");

    assertThat(response.senderId()).isEqualTo(9L);
  }

  @Test
  void send_reusesExistingThread() {
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.MENTEE)));
    MessageThread existing = mock(MessageThread.class);
    when(existing.getId()).thenReturn(55L);
    when(threadRepository.findByParticipantAAndParticipantB(2L, 9L))
        .thenReturn(Optional.of(existing));
    when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

    MessageResponse response = messageService.send(admin, 2L, "다시");

    assertThat(response.threadId()).isEqualTo(55L);
    verify(threadRepository, never()).save(any());
    verify(existing).touch(any());
  }

  // --- getThread ---

  @Test
  void getThread_nonParticipant_forbidden403() {
    MessageThread thread = mock(MessageThread.class);
    when(thread.hasParticipant(2L)).thenReturn(false);
    when(threadRepository.findById(50L)).thenReturn(Optional.of(thread));

    assertThatThrownBy(() -> messageService.getThread(mentee, 50L, PageRequest.of(0, 20)))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MESSAGING_FORBIDDEN);
    verify(messageRepository, never()).markThreadReadForReader(any(), any(), any());
  }

  @Test
  void getThread_notFound404() {
    when(threadRepository.findById(50L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> messageService.getThread(mentee, 50L, PageRequest.of(0, 20)))
        .isInstanceOf(NotFoundException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MESSAGING_THREAD_NOT_FOUND);
  }

  @Test
  void getThread_participant_marksReadAndReturnsTranscript() {
    MessageThread thread = mock(MessageThread.class);
    when(thread.hasParticipant(2L)).thenReturn(true);
    when(threadRepository.findById(50L)).thenReturn(Optional.of(thread));
    when(messageRepository.findByThreadIdOrderByCreatedAtAsc(eq(50L), any()))
        .thenReturn(new PageImpl<>(List.of(new Message(50L, 1L, "hi"))));

    PageResponse<MessageResponse> response =
        messageService.getThread(mentee, 50L, PageRequest.of(0, 20));

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).body()).isEqualTo("hi");
    verify(messageRepository).markThreadReadForReader(eq(50L), eq(2L), any());
  }

  // --- unread count ---

  @Test
  void unreadCount_returnsRepositoryCount() {
    when(messageRepository.countUnreadForUser(2L)).thenReturn(3L);

    assertThat(messageService.unreadCount(mentee).count()).isEqualTo(3L);
  }

  // --- listThreads ---

  @Test
  void listThreads_composesPartnerPreviewAndUnread() {
    MessageThread t = mock(MessageThread.class);
    when(t.getId()).thenReturn(100L);
    when(t.partnerOf(2L)).thenReturn(1L);
    when(threadRepository.findByParticipant(2L)).thenReturn(List.of(t));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.MENTOR)));
    when(messageRepository.findFirstByThreadIdOrderByCreatedAtDesc(100L))
        .thenReturn(Optional.of(new Message(100L, 1L, "최근 메시지")));
    when(messageRepository.countByThreadIdAndSenderIdNotAndReadAtIsNull(100L, 2L)).thenReturn(2);

    List<ThreadSummaryResponse> threads = messageService.listThreads(mentee);

    assertThat(threads).hasSize(1);
    assertThat(threads.get(0).partnerId()).isEqualTo(1L);
    assertThat(threads.get(0).partnerNickname()).isEqualTo("user1");
    assertThat(threads.get(0).lastMessageBody()).isEqualTo("최근 메시지");
    assertThat(threads.get(0).unreadCount()).isEqualTo(2);
  }

  // --- listRecipients ---

  @Test
  void listRecipients_mentor_activeMenteesPlusAdmins() {
    when(meetingService.meetingIdsOwnedBy(1L)).thenReturn(List.of(10L));
    when(enrollmentService.activeMenteeIdsForMeetings(List.of(10L))).thenReturn(List.of(2L, 3L));
    when(userRepository.findAllById(List.of(2L, 3L)))
        .thenReturn(List.of(user(2L, Role.MENTEE), user(3L, Role.MENTEE)));
    when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(user(9L, Role.ADMIN)));

    List<RecipientResponse> recipients = messageService.listRecipients(mentor);

    assertThat(recipients)
        .extracting(RecipientResponse::userId)
        .containsExactlyInAnyOrder(2L, 3L, 9L);
  }
}
