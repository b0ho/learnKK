package com.learnkk.meeting.repository;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.meeting.entity.Meeting;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  Page<Meeting> findByStatus(MeetingStatus status, Pageable pageable);

  Page<Meeting> findByMentorId(Long mentorId, Pageable pageable);

  /** Ids of every meeting owned by a mentor — a cross-module read for messaging authorization. */
  @Query("SELECT m.id FROM Meeting m WHERE m.mentorId = :mentorId")
  List<Long> findMeetingIdsByMentorId(@Param("mentorId") Long mentorId);

  /** Distinct owners of the given meetings — a cross-module read for messaging authorization. */
  @Query("SELECT DISTINCT m.mentorId FROM Meeting m WHERE m.id IN :meetingIds")
  List<Long> findMentorIdsByIdIn(@Param("meetingIds") Collection<Long> meetingIds);

  /**
   * Conditional transition. Updates status only when the meeting is still in the {@code from}
   * state; returns the number of rows affected (0 means the transition was illegal / lost a race).
   */
  @Modifying(clearAutomatically = true)
  @Query(
      "UPDATE Meeting m SET m.status = :to, m.rejectReason = :rejectReason "
          + "WHERE m.id = :id AND m.status = :from")
  int transitionStatus(
      @Param("id") Long id,
      @Param("from") MeetingStatus from,
      @Param("to") MeetingStatus to,
      @Param("rejectReason") String rejectReason);
}
