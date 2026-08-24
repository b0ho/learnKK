package com.learnkk.survey.repository;

import com.learnkk.survey.entity.Feedback;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

  List<Feedback> findByMeetingId(Long meetingId);

  Optional<Feedback> findByMeetingIdAndMenteeId(Long meetingId, Long menteeId);
}
