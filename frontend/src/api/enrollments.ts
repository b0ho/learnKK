import { request } from './client';
import type { ApplicantResponse, EnrollmentResponse } from './types';

export const enrollmentsApi = {
  /** Apply to a meeting (mentee, first-come-first-served). */
  apply(meetingId: number): Promise<EnrollmentResponse> {
    return request<EnrollmentResponse>(`/api/meetings/${meetingId}/enrollments`, {
      method: 'POST',
    });
  },

  /** Cancel the caller's own enrollment for a meeting. */
  cancel(meetingId: number): Promise<void> {
    return request<void>(`/api/meetings/${meetingId}/enrollments/mine`, { method: 'DELETE' });
  },

  /** The caller's own enrollments across every meeting. */
  listMine(): Promise<EnrollmentResponse[]> {
    return request<EnrollmentResponse[]>('/api/enrollments/mine');
  },

  /** The applicants of a meeting (owning mentor or admin). */
  listApplicants(meetingId: number): Promise<ApplicantResponse[]> {
    return request<ApplicantResponse[]>(`/api/meetings/${meetingId}/applicants`);
  },
};
