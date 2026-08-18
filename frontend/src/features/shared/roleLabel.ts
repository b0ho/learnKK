import type { Role } from '@/api';

const ROLE_LABELS: Record<Role, string> = {
  MENTOR: '멘토',
  MENTEE: '멘티',
  ADMIN: '관리자',
};

export function roleLabel(role: Role): string {
  return ROLE_LABELS[role] ?? role;
}
