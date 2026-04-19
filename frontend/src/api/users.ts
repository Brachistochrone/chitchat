import client from './client';
import type { User } from '../types/api';

export const usersApi = {
  getMe: () =>
    client.get<User>('/users/me'),

  updateProfile: (displayName: string) =>
    client.put<User>('/users/me', { displayName }),

  changePassword: (currentPassword: string, newPassword: string) =>
    client.put('/users/me/password', { currentPassword, newPassword }),

  deleteAccount: () =>
    client.delete('/users/me'),
};
