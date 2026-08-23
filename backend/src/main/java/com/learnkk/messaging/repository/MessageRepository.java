package com.learnkk.messaging.repository;

import com.learnkk.messaging.entity.Message;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

  /** A thread's transcript, oldest first (chat order). */
  Page<Message> findByThreadIdOrderByCreatedAtAsc(Long threadId, Pageable pageable);

  /** The latest message in a thread — used for the thread-list preview. */
  Optional<Message> findFirstByThreadIdOrderByCreatedAtDesc(Long threadId);

  /** Unread count within one thread for the given reader (messages the reader did not send). */
  int countByThreadIdAndSenderIdNotAndReadAtIsNull(Long threadId, Long readerId);

  /**
   * Idempotent bulk read-receipt: marks every message the reader received in a thread as read.
   * Returns the number of rows updated (0 when there was nothing new to read).
   */
  @Modifying(clearAutomatically = true)
  @Query(
      "UPDATE Message m SET m.readAt = :now "
          + "WHERE m.threadId = :threadId AND m.senderId <> :readerId AND m.readAt IS NULL")
  int markThreadReadForReader(
      @Param("threadId") Long threadId,
      @Param("readerId") Long readerId,
      @Param("now") OffsetDateTime now);

  /** Total unread messages the user has received across every thread (polling badge). */
  @Query(
      "SELECT COUNT(m) FROM Message m "
          + "WHERE m.readAt IS NULL AND m.senderId <> :userId AND m.threadId IN "
          + "(SELECT t.id FROM MessageThread t "
          + "WHERE t.participantA = :userId OR t.participantB = :userId)")
  long countUnreadForUser(@Param("userId") Long userId);
}
