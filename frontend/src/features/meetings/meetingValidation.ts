export interface MeetingFormValues {
  title: string;
  topic: string;
  weeks: string;
  recruitStart: string;
  recruitEnd: string;
  capacity: string;
  format: string;
  initialContent: string;
}

export type MeetingFieldErrors = Partial<Record<keyof MeetingFormValues, string>>;

export function emptyMeetingForm(): MeetingFormValues {
  return {
    title: '',
    topic: '',
    weeks: '',
    recruitStart: '',
    recruitEnd: '',
    capacity: '',
    format: '',
    initialContent: '',
  };
}

export function validateMeeting(values: MeetingFormValues): MeetingFieldErrors {
  const errors: MeetingFieldErrors = {};

  if (!values.title.trim()) {
    errors.title = '제목은 필수입니다.';
  }

  const weeks = Number(values.weeks);
  if (!values.weeks.trim() || Number.isNaN(weeks) || !Number.isInteger(weeks) || weeks <= 0) {
    errors.weeks = '기간(주)은 1 이상의 정수여야 합니다.';
  }

  const capacity = Number(values.capacity);
  if (
    !values.capacity.trim() ||
    Number.isNaN(capacity) ||
    !Number.isInteger(capacity) ||
    capacity <= 0
  ) {
    errors.capacity = '정원은 1 이상의 정수여야 합니다.';
  }

  if (values.recruitStart && values.recruitEnd) {
    const start = new Date(values.recruitStart).getTime();
    const end = new Date(values.recruitEnd).getTime();
    if (!Number.isNaN(start) && !Number.isNaN(end) && start > end) {
      errors.recruitEnd = '모집 종료는 시작 이후여야 합니다.';
    }
  }

  return errors;
}

/** Convert a datetime-local string to an ISO instant, or null when blank. */
export function toIsoOrNull(value: string): string | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}
