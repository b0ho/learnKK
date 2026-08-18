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
};
