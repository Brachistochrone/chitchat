import client from './client';
import type { AuthResponse } from '../types/api';

export const authApi = {
  register: (email: string, password: string, username: string) =>
    client.post<AuthResponse>('/auth/register', { email, password, username }),

  login: (email: string, password: string) =>
    client.post<AuthResponse>('/auth/login', { email, password }),

  logout: () =>
    client.post('/auth/logout'),

  requestPasswordReset: (email: string) =>
    client.post('/auth/password-reset/request', { email }),

  confirmPasswordReset: (token: string, newPassword: string) =>
    client.post('/auth/password-reset/confirm', { token, newPassword }),
};
