package com.learnkk.session.repository;

import com.learnkk.session.entity.MeetingSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSessionRepository extends JpaRepository<MeetingSession, Long> {

  /** 세션 목록(주차·예정시각 오름차순). 멘티/멘토/관리자 현황 read. */
  List<MeetingSession> findByMeetingIdOrderByWeekAscScheduledAtAsc(Long meetingId);

  /** 전체 예정 세션 수 S(출석율·수료 판정 분모, BR-U5-3). */
  int countByMeetingId(Long meetingId);
}
