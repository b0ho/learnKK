package com.learnkk.meeting.repository;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.meeting.entity.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  Page<Meeting> findByStatus(MeetingStatus status, Pageable pageable);

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
