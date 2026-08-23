import type { ErrorPayload } from './types';

/**
 * Korean fallback messages keyed by the backend's UPPER_SNAKE domain codes
 * (see com.learnkk.kernel.error.ErrorCodes). The server-provided `message` is
 * always preferred; this map only backstops missing or generic messages.
 */
export const ERROR_CODE_MESSAGES: Record<string, string> = {
  // Common / validation
  VALIDATION_FAILED: '입력값을 다시 확인해 주세요.',
  INVALID_SORT_FIELD: '정렬 기준이 올바르지 않습니다.',
  // Auth
  AUTH_UNAUTHENTICATED: '로그인이 필요합니다.',
  AUTH_INVALID_CREDENTIALS: '닉네임 또는 비밀번호가 올바르지 않습니다.',
  AUTH_SESSION_EXPIRED: '세션이 만료되었습니다. 다시 로그인해 주세요.',
  ADMIN_SIGNUP_FORBIDDEN: '관리자 계정은 가입할 수 없습니다.',
  DUPLICATE_EMPLOYEE_NO: '이미 등록된 사번입니다.',
  DUPLICATE_NICKNAME: '이미 사용 중인 닉네임입니다.',
  // User / profile
  USER_NOT_FOUND: '사용자를 찾을 수 없습니다.',
  PROFILE_FORBIDDEN: '본인의 프로필만 수정할 수 있습니다.',
  PROFILE_TAGS_LIMIT: '관심사 태그는 10개 이하여야 합니다.',
  PROFILE_INTRO_LIMIT: '소개는 500자 이하여야 합니다.',
  // Meeting
  MEETING_NOT_FOUND: '모임을 찾을 수 없습니다.',
  MEETING_FORBIDDEN: '권한이 없습니다.',
  MEETING_VALIDATION: '모임 정보를 다시 확인해 주세요.',
  MEETING_INVALID_TRANSITION: '현재 상태에서는 처리할 수 없습니다.',
  MEETING_QUESTIONS_LOCKED: '사전 설문은 더 이상 수정할 수 없습니다.',
  MEETING_SESSIONS_NOT_ENDED: '모든 세션이 종료되어야 완료할 수 있습니다.',
  // Enrollment
  ENROLLMENT_NOT_FOUND: '신청 내역을 찾을 수 없습니다.',
  ENROLLMENT_FORBIDDEN: '권한이 없습니다.',
  ENROLLMENT_FULL: '모집 정원이 마감되었습니다.',
  ENROLLMENT_DUPLICATE: '이미 신청한 모임입니다.',
  ENROLLMENT_NOT_OPEN: '모집 중인 모임이 아닙니다.',
  ENROLLMENT_CANCEL_FORBIDDEN: '모임이 시작된 이후에는 신청을 취소할 수 없습니다.',
  // Messaging
  MESSAGING_FORBIDDEN: '이 사용자에게 쪽지를 보낼 권한이 없습니다.',
  MESSAGING_SELF: '자기 자신에게는 쪽지를 보낼 수 없습니다.',
  MESSAGING_EMPTY_BODY: '쪽지 내용을 입력해 주세요.',
  MESSAGING_RECIPIENT_NOT_FOUND: '받는 사람을 찾을 수 없습니다.',
  MESSAGING_THREAD_NOT_FOUND: '대화를 찾을 수 없습니다.',
};

const GENERIC_MESSAGE = '요청을 처리하는 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.';

/** Structured error thrown by the API client for any non-2xx response. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details?: Record<string, unknown> | null;

  constructor(status: number, payload: Partial<ErrorPayload>) {
    const code = payload.code ?? 'UNKNOWN';
    // Prefer the server-provided Korean message; fall back to the code map.
    const message = payload.message?.trim() || ERROR_CODE_MESSAGES[code] || GENERIC_MESSAGE;
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = payload.details ?? null;
  }
}

/** Resolve the user-facing Korean message for an unknown thrown value. */
export function resolveErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return GENERIC_MESSAGE;
}

/** Narrowing helper: is this an ApiError carrying the given domain code? */
export function isApiErrorCode(error: unknown, code: string): boolean {
  return error instanceof ApiError && error.code === code;
}
