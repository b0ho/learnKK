package com.learnkk.survey.repository;

import com.learnkk.survey.entity.SurveyAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

  List<SurveyAnswer> findByMeetingIdAndMenteeId(Long meetingId, Long menteeId);

  Optional<SurveyAnswer> findByQuestionIdAndMenteeId(Long questionId, Long menteeId);

  List<SurveyAnswer> findByMeetingId(Long meetingId);
}
