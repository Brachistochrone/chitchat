import client from './client';
import type { UnreadCount } from '../types/api';

export const notificationsApi = {
  getUnreadCounts: () =>
    client.get<UnreadCount[]>('/notifications/unread'),

  markRoomRead: (roomId: number) =>
    client.post(`/rooms/${roomId}/messages/read`),
};
