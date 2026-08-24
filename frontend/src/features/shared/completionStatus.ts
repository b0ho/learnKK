import type { CompletionStatus } from '@/api';
import type { BadgeVariant } from './meetingStatus';

const STATUS_LABELS: Record<CompletionStatus, string> = {
  NOT_COMPLETED: '미수료',
  COMPLETION_CANDIDATE: '수료후보',
  COMPLETED: '수료확정',
};

const STATUS_VARIANTS: Record<CompletionStatus, BadgeVariant> = {
  NOT_COMPLETED: 'outline',
  COMPLETION_CANDIDATE: 'default',
  COMPLETED: 'secondary',
};

export function completionStatusLabel(status: CompletionStatus): string {
  return STATUS_LABELS[status] ?? status;
}

export function completionStatusVariant(status: CompletionStatus): BadgeVariant {
  return STATUS_VARIANTS[status] ?? 'outline';
}

/** Format an attendance rate (0..1) as a Korean percentage string. */
export function formatRate(rate: number): string {
  return `${Math.round(rate * 100)}%`;
}
