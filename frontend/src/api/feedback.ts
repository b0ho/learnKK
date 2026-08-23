import { request } from './client';
import type { FeedbackResponse } from './types';

export const feedbackApi = {
  /** Submit (or re-submit) course feedback (participating mentee). */
  submit(meetingId: number, content: string): Promise<FeedbackResponse> {
    return request<FeedbackResponse>(`/api/meetings/${meetingId}/feedback`, {
      method: 'POST',
      body: { content },
    });
  },

  /** List a meeting's course feedback (owning mentor or admin). */
  list(meetingId: number): Promise<FeedbackResponse[]> {
    return request<FeedbackResponse[]>(`/api/meetings/${meetingId}/feedback`);
  },
};
