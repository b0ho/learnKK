import { request } from './client';
import type { ProfileResponse, ProfileUpdateRequest } from './types';

export const usersApi = {
  getMyProfile(): Promise<ProfileResponse> {
    return request<ProfileResponse>('/api/users/me/profile');
  },

  updateMyProfile(payload: ProfileUpdateRequest): Promise<ProfileResponse> {
    return request<ProfileResponse>('/api/users/me/profile', { method: 'PUT', body: payload });
  },
};
