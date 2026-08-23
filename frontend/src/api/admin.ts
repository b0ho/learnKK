import { request } from './client';
import type { MeetingResponse } from './types';

export const adminApi = {
  approveMeeting(id: number): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/admin/meetings/${id}/approve`, { method: 'POST' });
  },

  rejectMeeting(id: number, reason: string): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/admin/meetings/${id}/reject`, {
      method: 'POST',
      body: { reason },
    });
  },

  /** T3/T4: confirm recruitment (proceed=true) or cancel (proceed=false, reason required). */
  confirmRecruitment(id: number, proceed: boolean, reason?: string): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/admin/meetings/${id}/confirm-recruitment`, {
      method: 'POST',
      body: { proceed, reason },
    });
  },

  /** T5: start the meeting (READY_TO_START -> IN_PROGRESS). */
  approveStart(id: number): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/admin/meetings/${id}/approve-start`, { method: 'POST' });
  },

  /** T6: complete the meeting (IN_PROGRESS -> COMPLETED). */
  complete(id: number): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/admin/meetings/${id}/complete`, { method: 'POST' });
  },
};
