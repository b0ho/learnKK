import { request } from './client';
import type {
  MessageResponse,
  PageResponse,
  RecipientResponse,
  ThreadSummaryResponse,
  UnreadCountResponse,
} from './types';

export const messagesApi = {
  /** Send a direct message to another user (server enforces the messaging boundary). */
  send(recipientId: number, body: string): Promise<MessageResponse> {
    return request<MessageResponse>('/api/messages', {
      method: 'POST',
      body: { recipientId, body },
    });
  },

  /** The caller's threads, most-recently-active first, with unread counts. */
  listThreads(): Promise<ThreadSummaryResponse[]> {
    return request<ThreadSummaryResponse[]>('/api/messages/threads');
  },

  /** A thread's transcript (marks received messages read as a side effect). */
  getThread(
    threadId: number,
    params: { page?: number; size?: number } = {},
  ): Promise<PageResponse<MessageResponse>> {
    return request<PageResponse<MessageResponse>>(`/api/messages/threads/${threadId}`, {
      query: { page: params.page, size: params.size },
    });
  },

  /** Total unread messages for the caller — polled to drive the badge. */
  unreadCount(): Promise<UnreadCountResponse> {
    return request<UnreadCountResponse>('/api/messages/unread-count');
  },

  /** Users the caller is permitted to message (FE picker). */
  listRecipients(): Promise<RecipientResponse[]> {
    return request<RecipientResponse[]>('/api/messages/recipients');
  },
};
