import client from './client';
import type { Message } from '../types/api';

export const messagesApi = {
  getRoomMessages: (roomId: number, before?: string, limit = 50) =>
    client.get<Message[]>(`/rooms/${roomId}/messages`, {
      params: { before, limit },
    }),

  sendRoomMessage: (roomId: number, content: string, replyToId?: number, attachmentIds?: number[]) =>
    client.post<Message>(`/rooms/${roomId}/messages`, { content, replyToId, attachmentIds }),

  editMessage: (messageId: number, content: string) =>
    client.put<Message>(`/messages/${messageId}`, { content }),

  deleteMessage: (messageId: number) =>
    client.delete(`/messages/${messageId}`),

  getPersonalMessages: (userId: number, before?: string, limit = 50) =>
    client.get<Message[]>(`/chats/${userId}/messages`, {
      params: { before, limit },
    }),
};
