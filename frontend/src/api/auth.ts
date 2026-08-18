import { request } from './client';
import type { LoginRequest, SessionResponse, SignupRequest, UserResponse } from './types';

export const authApi = {
  signup(payload: SignupRequest): Promise<UserResponse> {
    return request<UserResponse>('/api/auth/signup', { method: 'POST', body: payload, auth: false });
  },

  login(payload: LoginRequest): Promise<SessionResponse> {
    return request<SessionResponse>('/api/auth/login', { method: 'POST', body: payload, auth: false });
  },

  logout(): Promise<void> {
    return request<void>('/api/auth/logout', { method: 'POST' });
  },
};
