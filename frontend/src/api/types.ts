// Domain types mirroring the backend OpenAPI contract (/contracts/openapi.yaml).
// Field names are camelCase to match the Jackson serialization on the wire.

export type Role = 'MENTOR' | 'MENTEE' | 'ADMIN';

export type MeetingStatus =
  | 'PENDING_APPROVAL'
  | 'RECRUITING'
  | 'READY_TO_START'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELLED';

/** Survey question type discriminator used by the builder and backend. */
export type SurveyQuestionType = 'SHORT_TEXT' | 'LONG_TEXT' | 'CHOICE';

/** Enrollment lifecycle status (U4). */
export type EnrollmentStatus = 'APPLIED' | 'CANCELLED';

export interface ErrorPayload {
  code: string;
  message: string;
  details?: Record<string, unknown> | null;
}

export interface SignupRequest {
  nickname: string;
  password: string;
  employeeNo: string;
  role: Exclude<Role, 'ADMIN'>;
}

export interface UserResponse {
  id: number;
  nickname: string;
  employeeNo: string;
  role: Role;
}

export interface LoginRequest {
  nickname: string;
  password: string;
}

export interface SessionResponse {
  token: string;
  role: Role;
}

export interface ProfileResponse {
  nickname: string;
  employeeNo: string;
  tags: string[];
  intro?: string | null;
}

export interface ProfileUpdateRequest {
  tags: string[];
  intro?: string | null;
}

export interface MeetingCreateRequest {
  title: string;
  topic?: string | null;
  weeks: number;
  recruitStart?: string | null;
  recruitEnd?: string | null;
  capacity: number;
  format?: string | null;
  initialContent?: string | null;
}

export interface MeetingResponse {
  id: number;
  mentorId: number;
  title: string;
  topic?: string | null;
  weeks: number;
  recruitStart?: string | null;
  recruitEnd?: string | null;
  capacity: number;
  format?: string | null;
  initialContent?: string | null;
  status: MeetingStatus;
  rejectReason?: string | null;
}

export interface MeetingSummary {
  id: number;
  title: string;
  topic?: string | null;
  weeks: number;
  capacity: number;
  status: MeetingStatus;
}

export interface SurveyQuestionDto {
  orderNo: number;
  text: string;
  type: string;
  options?: string[];
  required?: boolean;
}

export interface RejectRequest {
  reason: string;
}

export interface ConfirmRecruitmentRequest {
  proceed: boolean;
  reason?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EnrollmentResponse {
  id: number;
  meetingId: number;
  menteeId: number;
  status: EnrollmentStatus;
  appliedAt: string;
}

export interface ApplicantResponse {
  menteeId: number;
  nickname?: string | null;
  appliedAt: string;
}

// --- Session / attendance / completion (U5, Bolt 6) ---

/** Mentee completion status (contract #3). */
export type CompletionStatus = 'NOT_COMPLETED' | 'COMPLETION_CANDIDATE' | 'COMPLETED';

export interface CreateSessionRequest {
  week: number;
  scheduledAt: string;
  checkInWindowMinutes?: number | null;
}

export interface UpdateSessionRequest {
  scheduledAt: string;
}

/**
 * A meeting session (U5). Named MeetingSessionResponse to avoid colliding with the auth
 * SessionResponse (login token) type — mirrors the OpenAPI MeetingSessionResponse schema.
 */
export interface MeetingSessionResponse {
  id: number;
  meetingId: number;
  week: number;
  scheduledAt: string;
  checkInWindowMinutes: number;
}

export interface AttendanceResponse {
  sessionId: number;
  menteeId: number;
  checkedInAt: string;
}

export interface AttendanceSummaryResponse {
  attended: number;
  totalScheduled: number;
  rate: number;
}

export interface MenteeCompletionResponse {
  meetingId: number;
  menteeId: number;
  status: CompletionStatus;
  attendedCount: number;
  totalScheduled: number;
  approvedAt?: string | null;
}
