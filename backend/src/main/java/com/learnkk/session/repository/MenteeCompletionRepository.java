package com.learnkk.session.repository;

import com.learnkk.session.entity.MenteeCompletion;
import com.learnkk.session.entity.MenteeCompletionId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenteeCompletionRepository
    extends JpaRepository<MenteeCompletion, MenteeCompletionId> {

  Optional<MenteeCompletion> findByMeetingIdAndMenteeId(Long meetingId, Long menteeId);

  List<MenteeCompletion> findByMeetingId(Long meetingId);
}
