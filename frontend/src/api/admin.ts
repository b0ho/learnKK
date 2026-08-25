import { request } from './client';
import type {
  ApprovalQueues,
  MeetingMonitorRow,
  MeetingResponse,
  MeetingSummary,
  MeetingStatus,
  PageResponse,
} from './types';

export const adminApi = {
  /** FR-2/FR-3: 상태별 모임 목록(관리자 승인 큐). */
  listByStatus(status: MeetingStatus, params: { size?: number } = {}): Promise<PageResponse<MeetingSummary>> {
    return request<PageResponse<MeetingSummary>>('/api/admin/meetings', {
      query: { status, size: params.size },
    });
  },

  /** US-9.1: 승인 큐 집계(5개 큐, 조회 전용). */
  queues(): Promise<ApprovalQueues> {
    return request<ApprovalQueues>('/api/admin/queues');
  },

  /** US-9.2: 운영 현황 모니터링(모임별 현황). */
  monitoring(): Promise<MeetingMonitorRow[]> {
    return request<MeetingMonitorRow[]>('/api/admin/monitoring');
  },

  /** FR-5: 승인 되돌리기(직전 상태로 역전이). */
  revert(id: number): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/admin/meetings/${id}/revert`, { method: 'POST' });
  },

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

  /** FR-7: 멘토 수료 판정(수료/미수료, 관리자 판단만). */
  judgeMentorCompletion(
    id: number,
    status: 'COMPLETED' | 'NOT_COMPLETED',
  ): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/admin/meetings/${id}/mentor-completion`, {
      method: 'POST',
      body: { status },
    });
  },
};
