import { describe, expect, it } from 'vitest';
import { roleLabel } from './roleLabel';
import { meetingStatusLabel, meetingStatusVariant } from './meetingStatus';

describe('roleLabel', () => {
  it('maps known roles to Korean', () => {
    expect(roleLabel('MENTOR')).toBe('멘토');
    expect(roleLabel('MENTEE')).toBe('멘티');
    expect(roleLabel('ADMIN')).toBe('관리자');
  });
});

describe('meetingStatus helpers', () => {
  it('labels statuses in Korean', () => {
    expect(meetingStatusLabel('RECRUITING')).toBe('모집중');
    expect(meetingStatusLabel('PENDING_APPROVAL')).toBe('승인대기');
    expect(meetingStatusLabel('REJECTED')).toBe('반려');
  });

  it('maps statuses to badge variants', () => {
    expect(meetingStatusVariant('RECRUITING')).toBe('default');
    expect(meetingStatusVariant('REJECTED')).toBe('destructive');
    expect(meetingStatusVariant('PENDING_APPROVAL')).toBe('outline');
  });
});
