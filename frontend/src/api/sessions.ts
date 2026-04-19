import client from './client';
import type { Session } from '../types/api';

export const sessionsApi = {
  getActiveSessions: () =>
    client.get<Session[]>('/sessions'),

  revokeSession: (sessionId: number) =>
    client.delete(`/sessions/${sessionId}`),
};
