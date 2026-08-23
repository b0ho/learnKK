package com.learnkk.messaging.service;

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
import com.learnkk.messaging.dto.UnreadCountResponse;
import com.learnkk.messaging.entity.Message;
import com.learnkk.messaging.entity.MessageThread;
import com.learnkk.messaging.repository.MessageRepository;
import com.learnkk.messaging.repository.MessageThreadRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direct messaging (C6, U7): 1:1 threads, send, transcript read (with idempotent read-receipt) and
 * the polling unread count. The permission boundary — who may message whom — is enforced
 * server-side in {@link #canMessage} and re-checked on every send (BR: MESSAGING_FORBIDDEN → 403).
 *
 * <p>Messaging never touches the meetings or enrollment tables directly: the mentor↔mentee
 * relationship is read through {@link MeetingService} and {@link EnrollmentService} (ADR-007).
 */
@Service
public class MessageService {

  private final MessageThreadRepository threadRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final MeetingService meetingService;
  private final EnrollmentService enrollmentService;

  public MessageService(
      MessageThreadRepository threadRepository,
      MessageRepository messageRepository,
      UserRepository userRepository,
      MeetingService meetingService,
      EnrollmentService enrollmentService) {
    this.threadRepository = threadRepository;
    this.messageRepository = messageRepository;
    this.userRepository = userRepository;
    this.meetingService = meetingService;
    this.enrollmentService = enrollmentService;
  }

  /**
   * Send a direct message. Empty body → 400; self-send → 400; unknown recipient → 404; a recipient
   * outside the caller's messaging boundary → 403. The (sender, recipient) thread is found or
   * created (normalized pair) and its last-activity timestamp advanced.
   */
  @Transactional
  public MessageResponse send(Principal sender, Long recipientId, String body) {
    String trimmed = body == null ? "" : body.trim();
    if (trimmed.isEmpty()) {
      throw new ValidationException(ErrorCodes.MESSAGING_EMPTY_BODY, "쪽지 내용을 입력해 주세요.");
    }
    if (recipientId.equals(sender.userId())) {
      throw new ValidationException(ErrorCodes.MESSAGING_SELF, "자기 자신에게는 쪽지를 보낼 수 없습니다.");
    }

    User recipient =
        userRepository
            .findById(recipientId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        ErrorCodes.MESSAGING_RECIPIENT_NOT_FOUND, "받는 사람을 찾을 수 없습니다."));

    if (!canMessage(sender, recipient)) {
      throw new ForbiddenException(ErrorCodes.MESSAGING_FORBIDDEN, "이 사용자에게 쪽지를 보낼 권한이 없습니다.");
    }

    MessageThread thread = findOrCreateThread(sender.userId(), recipientId);
    Message saved = messageRepository.save(new Message(thread.getId(), sender.userId(), trimmed));
    thread.touch(OffsetDateTime.now()); // managed entity — flushed by dirty checking
    return MessageResponse.from(saved);
  }

  /**
   * Find the normalized-pair thread or create it. Two simultaneous first messages between the same
   * pair would both see no thread and both insert; the loser hits the UNIQUE(participant_a,
   * participant_b) constraint, so we re-read the now-committed thread instead of surfacing a 500
   * (same DataIntegrityViolation recovery pattern as EnrollmentService.apply / AuthService.signup).
   */
  private MessageThread findOrCreateThread(Long userX, Long userY) {
    long[] pair = MessageThread.normalizedPair(userX, userY);
    return threadRepository
        .findByParticipantAAndParticipantB(pair[0], pair[1])
        .orElseGet(
            () -> {
              try {
                return threadRepository.saveAndFlush(MessageThread.of(userX, userY));
              } catch (DataIntegrityViolationException lostRace) {
                return threadRepository
                    .findByParticipantAAndParticipantB(pair[0], pair[1])
                    .orElseThrow(() -> lostRace);
              }
            });
  }

  /** The caller's threads (most-recently-active first) with partner, latest preview and unread. */
  @Transactional(readOnly = true)
  public List<ThreadSummaryResponse> listThreads(Principal principal) {
    Long userId = principal.userId();
    return threadRepository.findByParticipant(userId).stream()
        .map(
            t -> {
              Long partnerId = t.partnerOf(userId);
              String nickname =
                  userRepository.findById(partnerId).map(User::getNickname).orElse(null);
              String lastBody =
                  messageRepository
                      .findFirstByThreadIdOrderByCreatedAtDesc(t.getId())
                      .map(Message::getBody)
                      .orElse(null);
              int unread =
                  messageRepository.countByThreadIdAndSenderIdNotAndReadAtIsNull(t.getId(), userId);
              return new ThreadSummaryResponse(
                  t.getId(), partnerId, nickname, lastBody, t.getLastMessageAt(), unread);
            })
        .toList();
  }

  /**
   * The transcript of a thread the caller participates in (oldest first). Reading a thread marks
   * every message the caller received as read (idempotent — only rows still unread are touched); a
   * non-participant gets 403.
   */
  @Transactional
  public PageResponse<MessageResponse> getThread(
      Principal principal, Long threadId, Pageable page) {
    MessageThread thread =
        threadRepository
            .findById(threadId)
            .orElseThrow(
                () ->
                    new NotFoundException(ErrorCodes.MESSAGING_THREAD_NOT_FOUND, "대화를 찾을 수 없습니다."));
    if (!thread.hasParticipant(principal.userId())) {
      throw new ForbiddenException(ErrorCodes.MESSAGING_FORBIDDEN, "이 대화를 열람할 권한이 없습니다.");
    }

    messageRepository.markThreadReadForReader(threadId, principal.userId(), OffsetDateTime.now());
    return PageResponse.from(
        messageRepository
            .findByThreadIdOrderByCreatedAtAsc(threadId, page)
            .map(MessageResponse::from));
  }

  /** Total unread messages the caller has received across every thread — drives the badge. */
  @Transactional(readOnly = true)
  public UnreadCountResponse unreadCount(Principal principal) {
    return new UnreadCountResponse(messageRepository.countUnreadForUser(principal.userId()));
  }

  /**
   * Users the caller is permitted to message — supports the FE recipient picker. The server's
   * {@link #send} re-validates via {@link #canMessage}, so this list is UX assistance only.
   */
  @Transactional(readOnly = true)
  public List<RecipientResponse> listRecipients(Principal principal) {
    Long userId = principal.userId();
    Map<Long, User> byId = new LinkedHashMap<>();

    if (principal.isAdmin()) {
      // Admins may message everyone.
      userRepository.findByIdNot(userId).forEach(u -> byId.put(u.getId(), u));
    } else if (principal.isMentor()) {
      // Active mentees across the mentor's own meetings, plus admins.
      List<Long> meetingIds = meetingService.meetingIdsOwnedBy(userId);
      List<Long> menteeIds = enrollmentService.activeMenteeIdsForMeetings(meetingIds);
      userRepository.findAllById(menteeIds).forEach(u -> byId.put(u.getId(), u));
      userRepository.findByRole(Role.ADMIN).forEach(u -> byId.put(u.getId(), u));
    } else {
      // Mentee: mentors of the meetings the mentee applied to, plus admins.
      List<Long> meetingIds = enrollmentService.activeMeetingIdsForMentee(userId);
      List<Long> mentorIds = meetingService.mentorIdsForMeetings(meetingIds);
      userRepository.findAllById(mentorIds).forEach(u -> byId.put(u.getId(), u));
      userRepository.findByRole(Role.ADMIN).forEach(u -> byId.put(u.getId(), u));
    }
    byId.remove(userId); // never message yourself

    return byId.values().stream()
        .map(u -> new RecipientResponse(u.getId(), u.getNickname(), u.getRole()))
        .toList();
  }

  /**
   * The messaging boundary. ADMIN may message anyone and anyone may message an ADMIN. Otherwise a
   * mentor may message a mentee actively enrolled in one of the mentor's meetings, and a mentee may
   * message the mentor of a meeting the mentee applied to. Reads U3/U4 through their services.
   */
  private boolean canMessage(Principal sender, User recipient) {
    if (recipient.getId().equals(sender.userId())) {
      return false;
    }
    if (sender.isAdmin() || recipient.getRole() == Role.ADMIN) {
      return true;
    }
    if (sender.isMentor()) {
      return enrollmentService.isActivelyEnrolledInAnyOf(
          meetingService.meetingIdsOwnedBy(sender.userId()), recipient.getId());
    }
    if (sender.isMentee()) {
      List<Long> meetingIds = enrollmentService.activeMeetingIdsForMentee(sender.userId());
      return meetingService.mentorIdsForMeetings(meetingIds).contains(recipient.getId());
    }
    return false;
  }
}
