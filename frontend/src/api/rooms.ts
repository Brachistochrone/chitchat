import client from './client';
import type { Room, MemberResponse } from '../types/api';

export const roomsApi = {
  searchPublicRooms: (query = '', page = 0, size = 50) =>
    client.get<{ content: Room[]; totalElements: number }>('/rooms', {
      params: { q: query, page, size },
    }),

  createRoom: (name: string, description: string | null, visibility: 'PUBLIC' | 'PRIVATE') =>
    client.post<Room>('/rooms', { name, description, visibility }),

  getRoom: (roomId: number) =>
    client.get<Room>(`/rooms/${roomId}`),

  joinRoom: (roomId: number) =>
    client.post(`/rooms/${roomId}/join`),

  leaveRoom: (roomId: number) =>
    client.post(`/rooms/${roomId}/leave`),

  getMembers: (roomId: number) =>
    client.get<MemberResponse[]>(`/rooms/${roomId}/members`),
};
