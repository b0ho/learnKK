import { request } from './client';
import type {
  MeetingCreateRequest,
  MeetingResponse,
  MeetingSummary,
  PageResponse,
  SurveyQuestionDto,
} from './types';

interface ListRecruitingParams {
  page?: number;
  size?: number;
  sort?: string;
}

export const meetingsApi = {
  create(payload: MeetingCreateRequest): Promise<MeetingResponse> {
    return request<MeetingResponse>('/api/meetings', { method: 'POST', body: payload });
  },

  get(id: number): Promise<MeetingResponse> {
    return request<MeetingResponse>(`/api/meetings/${id}`, { auth: false });
  },

  listRecruiting(params: ListRecruitingParams = {}): Promise<PageResponse<MeetingSummary>> {
    return request<PageResponse<MeetingSummary>>('/api/meetings', {
      auth: false,
      query: { status: 'recruiting', page: params.page, size: params.size, sort: params.sort },
    });
  },

  getQuestions(id: number): Promise<SurveyQuestionDto[]> {
    return request<SurveyQuestionDto[]>(`/api/meetings/${id}/questions`, { auth: false });
  },

  putQuestions(id: number, questions: SurveyQuestionDto[]): Promise<SurveyQuestionDto[]> {
    return request<SurveyQuestionDto[]>(`/api/meetings/${id}/questions`, {
      method: 'PUT',
      body: questions,
    });
  },
};
