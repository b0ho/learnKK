-- V13: 멘토 수료 판정 컬럼 추가 (ux-bugfixes-2 FR-7).
--
-- 멘티 수료(mentee_completion, 출석 80% 자동 판정 + 관리자 확정)와 별개로, 관리자가 모임의 멘토에 대해
-- '수료/미수료'를 판단만으로 판정한다. 초기값 PENDING(판정 전). 불변 마이그레이션 원칙에 따라 신규 V13으로 추가한다.

ALTER TABLE meetings
  ADD COLUMN IF NOT EXISTS mentor_completion_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

-- 데모 시드: 완료된 CS 스터디의 멘토는 정상 완료로 수료 처리(멱등).
UPDATE meetings
SET mentor_completion_status = 'COMPLETED'
WHERE title = '완료된 CS 스터디' AND mentor_completion_status = 'PENDING';
