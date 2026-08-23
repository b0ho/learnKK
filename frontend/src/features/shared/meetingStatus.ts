import type { MeetingStatus } from '@/api';

const STATUS_LABELS: Record<MeetingStatus, string> = {
  PENDING_APPROVAL: '승인대기',
  RECRUITING: '모집중',
  READY_TO_START: '시작대기',
  IN_PROGRESS: '진행중',
  COMPLETED: '완료',
  REJECTED: '반려',
  CANCELLED: '취소',
};

export type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline';

const STATUS_VARIANTS: Record<MeetingStatus, BadgeVariant> = {
  PENDING_APPROVAL: 'outline',
  RECRUITING: 'default',
  READY_TO_START: 'secondary',
  IN_PROGRESS: 'secondary',
  COMPLETED: 'secondary',
  REJECTED: 'destructive',
  CANCELLED: 'destructive',
};

export function meetingStatusLabel(status: MeetingStatus): string {
  return STATUS_LABELS[status] ?? status;
}

export function meetingStatusVariant(status: MeetingStatus): BadgeVariant {
  return STATUS_VARIANTS[status] ?? 'secondary';
}
