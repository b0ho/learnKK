import { request } from './client';
import type { SurveyAnswerItem, SurveyAnswerResponse } from './types';

export const surveyApi = {
  /** Submit (or re-submit) the caller's pre-application survey answers (mentee). */
  submitAnswers(meetingId: number, answers: SurveyAnswerItem[]): Promise<SurveyAnswerResponse[]> {
    return request<SurveyAnswerResponse[]>(`/api/meetings/${meetingId}/survey-answers`, {
      method: 'POST',
      body: { answers },
    });
  },

  /** Read the caller's own pre-application survey answers. */
  getMyAnswers(meetingId: number): Promise<SurveyAnswerResponse[]> {
    return request<SurveyAnswerResponse[]>(`/api/meetings/${meetingId}/survey-answers/mine`);
  },

  /** Read a mentee's pre-application survey answers (owning mentor or admin). */
  getMenteeAnswers(meetingId: number, menteeId: number): Promise<SurveyAnswerResponse[]> {
    return request<SurveyAnswerResponse[]>(
      `/api/meetings/${meetingId}/mentees/${menteeId}/survey-answers`,
    );
  },
};
