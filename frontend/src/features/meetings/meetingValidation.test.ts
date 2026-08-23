import { describe, expect, it } from 'vitest';
import { emptyMeetingForm, toIsoOrNull, validateMeeting } from './meetingValidation';

describe('validateMeeting', () => {
  const base = { ...emptyMeetingForm(), title: '스터디', weeks: '4', capacity: '6' };

  it('accepts a valid form', () => {
    expect(validateMeeting(base)).toEqual({});
  });

  it('requires a title', () => {
    expect(validateMeeting({ ...base, title: '  ' }).title).toBeDefined();
  });

  it('rejects non-positive weeks and capacity', () => {
    expect(validateMeeting({ ...base, weeks: '0' }).weeks).toBeDefined();
    expect(validateMeeting({ ...base, weeks: 'abc' }).weeks).toBeDefined();
    expect(validateMeeting({ ...base, capacity: '-1' }).capacity).toBeDefined();
  });

  it('rejects an inverted recruit period', () => {
    const errors = validateMeeting({
      ...base,
      recruitStart: '2024-05-10T10:00',
      recruitEnd: '2024-05-01T10:00',
    });
    expect(errors.recruitEnd).toBeDefined();
  });
});

describe('toIsoOrNull', () => {
  it('returns null for blank', () => {
    expect(toIsoOrNull('')).toBeNull();
  });

  it('converts a datetime-local string to ISO', () => {
    const iso = toIsoOrNull('2024-05-01T10:00');
    expect(iso).toMatch(/^2024-05-01T/);
    expect(iso?.endsWith('Z')).toBe(true);
  });
});
