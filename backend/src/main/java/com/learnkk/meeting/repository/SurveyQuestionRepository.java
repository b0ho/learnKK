package com.learnkk.meeting.repository;

import com.learnkk.meeting.entity.SurveyQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

  List<SurveyQuestion> findByMeetingIdOrderByOrderNoAsc(Long meetingId);

  @Transactional
  void deleteByMeetingId(Long meetingId);
}
