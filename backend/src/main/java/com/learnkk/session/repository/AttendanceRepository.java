package com.learnkk.session.repository;

import com.learnkk.session.entity.Attendance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

  Optional<Attendance> findBySessionIdAndMenteeId(Long sessionId, Long menteeId);

  boolean existsBySessionIdAndMenteeId(Long sessionId, Long menteeId);

  /**
   * 특정 모임에서 한 멘티가 출석한 세션 수 a 를 집계한다(출석율·수료 판정, BR-U5-3/4). attendance 를 meeting_session 과
   * join 해 해당 모임의 세션에 대한 출석만 센다.
   */
  @Query(
      "SELECT COUNT(a) FROM Attendance a, MeetingSession s "
          + "WHERE a.sessionId = s.id AND s.meetingId = :meetingId AND a.menteeId = :menteeId")
  long countAttendedSessions(@Param("meetingId") Long meetingId, @Param("menteeId") Long menteeId);

  /**
   * 해당 모임에서 한 멘티가 출석한 세션 id 목록(FR-5, 출석완료 상태 유지 표시). 세션 목록과 결합해 출석한 세션을
   * '출석완료'로 지속 표시하는 데 쓴다.
   */
  @Query(
      "SELECT a.sessionId FROM Attendance a, MeetingSession s "
          + "WHERE a.sessionId = s.id AND s.meetingId = :meetingId AND a.menteeId = :menteeId")
  java.util.List<Long> findAttendedSessionIds(
      @Param("meetingId") Long meetingId, @Param("menteeId") Long menteeId);
}
