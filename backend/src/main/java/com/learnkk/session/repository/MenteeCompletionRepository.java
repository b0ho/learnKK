package com.learnkk.session.repository;

import com.learnkk.kernel.domain.CompletionStatus;
import com.learnkk.session.entity.MenteeCompletion;
import com.learnkk.session.entity.MenteeCompletionId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenteeCompletionRepository
    extends JpaRepository<MenteeCompletion, MenteeCompletionId> {

  Optional<MenteeCompletion> findByMeetingIdAndMenteeId(Long meetingId, Long menteeId);

  List<MenteeCompletion> findByMeetingId(Long meetingId);

  /** All completion rows in a given status — admin ④ 멘티 수료 대기 큐 집계(U9 read port). */
  List<MenteeCompletion> findByStatus(CompletionStatus status);
}
