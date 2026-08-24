import { request } from './client';
import type {
  AttendanceResponse,
  AttendanceSummaryResponse,
  CreateSessionRequest,
  MeetingSessionResponse,
  MenteeCompletionResponse,
  UpdateSessionRequest,
} from './types';

/**
 * Session / attendance / completion API (U5, Bolt 6). File named sessions.ts to avoid clashing with
 * the auth session.ts (token storage). Mirrors the /contracts/openapi.yaml routes.
 */
export const sessionsApi = {
  /** List a meeting's sessions (week/scheduledAt ascending). */
  listSessions(meetingId: number): Promise<MeetingSessionResponse[]> {
    return request<MeetingSessionResponse[]>(`/api/meetings/${meetingId}/sessions`);
  },

  /** Add a session to a meeting (owning mentor, meeting IN_PROGRESS). */
  addSession(meetingId: number, body: CreateSessionRequest): Promise<MeetingSessionResponse> {
    return request<MeetingSessionResponse>(`/api/meetings/${meetingId}/sessions`, {
      method: 'POST',
      body,
    });
  },

  /** Reschedule a session (owning mentor). */
  updateSession(sessionId: number, body: UpdateSessionRequest): Promise<MeetingSessionResponse> {
    return request<MeetingSessionResponse>(`/api/sessions/${sessionId}`, {
      method: 'PUT',
      body,
    });
  },

  /** Delete a session (owning mentor, FR-7). Attendance cascades. */
  deleteSession(sessionId: number): Promise<void> {
    return request<void>(`/api/sessions/${sessionId}`, { method: 'DELETE' });
  },

  /** Mark a session complete (owning mentor, FR-8). */
  completeSession(sessionId: number): Promise<MeetingSessionResponse> {
    return request<MeetingSessionResponse>(`/api/sessions/${sessionId}/complete`, {
      method: 'POST',
    });
  },

  /** Pop-up self check-in for a session (participant mentee, within the time window). */
  checkIn(sessionId: number): Promise<AttendanceResponse> {
    return request<AttendanceResponse>(`/api/sessions/${sessionId}/attendance`, {
      method: 'POST',
    });
  },

  /** The caller's attendance summary (attended / totalScheduled / rate) for a meeting. */
  getMyAttendance(meetingId: number): Promise<AttendanceSummaryResponse> {
    return request<AttendanceSummaryResponse>(`/api/meetings/${meetingId}/my-attendance`);
  },

  /** Run the 80% completion auto-judgement (owning mentor or admin). */
  computeCompletions(meetingId: number): Promise<MenteeCompletionResponse[]> {
    return request<MenteeCompletionResponse[]>(`/api/meetings/${meetingId}/completions/compute`, {
      method: 'POST',
    });
  },

  /** List a meeting's completion judgements (owning mentor or admin). */
  listCompletions(meetingId: number): Promise<MenteeCompletionResponse[]> {
    return request<MenteeCompletionResponse[]>(`/api/meetings/${meetingId}/completions`);
  },

  /** Approve a mentee's completion (④, admin only). */
  approveCompletion(meetingId: number, menteeId: number): Promise<MenteeCompletionResponse> {
    return request<MenteeCompletionResponse>(
      `/api/admin/meetings/${meetingId}/completions/${menteeId}/approve`,
      { method: 'POST' },
    );
  },
};
