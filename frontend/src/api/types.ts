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

/** 멘토 수료 판정(FR-7): 관리자 판단만으로 결정. PENDING=판정 전. */
export type MentorCompletionStatus = 'PENDING' | 'COMPLETED' | 'NOT_COMPLETED';

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
  /** FR-7: 백엔드는 항상 반환(기본 PENDING). FE에서는 선택적으로 두어 목 데이터 호환성을 유지한다. */
  mentorCompletionStatus?: MentorCompletionStatus;
}

export interface MeetingSummary {
  id: number;
  title: string;
  topic?: string | null;
  weeks: number;
  capacity: number;
  status: MeetingStatus;
  /** FR-7: 백엔드는 항상 반환(기본 PENDING). FE에서는 선택적으로 두어 목 데이터 호환성을 유지한다. */
  mentorCompletionStatus?: MentorCompletionStatus;
}

/** US-9.2(U9): 관리자 운영 현황 모니터링 행 — 모임별 상태·출석율(세션 기준)·수료 진행. */
export interface MeetingMonitoringSummary {
  id: number;
  title: string;
  status: MeetingStatus;
  mentorId: number;
  mentorNickname?: string | null;
  /** 참여(APPLIED) 멘티 수. */
  menteeCount: number;
  /** 전체 예정 세션 수 S. */
  sessionCount: number;
  /** 종료된 세션 수(수동 완료 또는 시간창 경과). */
  endedSessionCount: number;
  /** 세션 기준 출석율(0..1) — 총 출석 수 / (S × 멘티 수), 분모 0이면 0. */
  attendanceRate: number;
  completedMenteeCount: number;
  completionCandidateCount: number;
  mentorCompletionStatus?: MentorCompletionStatus;
}

export interface SurveyQuestionDto {
  /** Populated on read; the upsert/write path ignores it. */
  id?: number | null;
  orderNo: number;
  text: string;
  type: string;
  options?: string[];
  required?: boolean;
}

/** A single pre-application survey answer (U8). */
export interface SurveyAnswerItem {
  questionId: number;
  answerText?: string | null;
}

export interface SurveyAnswerRequest {
  answers: SurveyAnswerItem[];
}

export interface SurveyAnswerResponse {
  questionId: number;
  answerText?: string | null;
}

export interface FeedbackRequest {
  content: string;
}

export interface FeedbackResponse {
  id: number;
  menteeId: number;
  content: string;
  createdAt: string;
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

// --- Content (U6): week posts, attachments, notices ---

export interface AttachmentResponse {
  id: number;
  postId: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploaderId?: number | null;
  createdAt: string;
}

export interface PostResponse {
  id: number;
  meetingId: number;
  authorId: number;
  week: number;
  body: string;
  attachments: AttachmentResponse[];
  createdAt: string;
  updatedAt?: string | null;
}

export interface PostCreateRequest {
  week: number;
  body: string;
}

export interface NoticeResponse {
  id: number;
  meetingId: number;
  authorId: number;
  body: string;
  createdAt: string;
}

export interface NoticeCreateRequest {
  body: string;
}

// --- Messaging (U7) ---

export interface SendMessageRequest {
  recipientId: number;
  body: string;
}

export interface MessageResponse {
  id: number;
  threadId: number;
  senderId: number;
  body: string;
  readAt?: string | null;
  createdAt: string;
}

export interface ThreadSummaryResponse {
  threadId: number;
  partnerId: number;
  partnerNickname?: string | null;
  lastMessageBody?: string | null;
  lastMessageAt?: string | null;
  unreadCount: number;
}

export interface UnreadCountResponse {
  count: number;
}

export interface RecipientResponse {
  userId: number;
  nickname: string;
  role: Role;
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
  completed: boolean;
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
  /** 출석한 세션 id 목록(FR-5): 출석완료 상태를 재방문·시간창 종료 후에도 유지 표시하는 데 사용. */
  attendedSessionIds: number[];
}

export interface MenteeCompletionResponse {
  meetingId: number;
  menteeId: number;
  status: CompletionStatus;
  attendedCount: number;
  totalScheduled: number;
  approvedAt?: string | null;
}
