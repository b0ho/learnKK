package com.learnkk.messaging.repository;

import com.learnkk.messaging.entity.MessageThread;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {

  /** Look up the (already normalized) thread for a participant pair. */
  Optional<MessageThread> findByParticipantAAndParticipantB(Long participantA, Long participantB);

  /** All threads the user participates in, most-recently-active first. */
  @Query(
      "SELECT t FROM MessageThread t "
          + "WHERE t.participantA = :userId OR t.participantB = :userId "
          + "ORDER BY t.lastMessageAt DESC NULLS LAST, t.id DESC")
  List<MessageThread> findByParticipant(@Param("userId") Long userId);
}
