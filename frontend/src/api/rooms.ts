import client from './client';
import type { Room, MemberResponse, BanResponse } from '../types/api';

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

  updateRoom: (roomId: number, name?: string, description?: string, visibility?: 'PUBLIC' | 'PRIVATE') =>
    client.put<Room>(`/rooms/${roomId}`, { name, description, visibility }),

  deleteRoom: (roomId: number) =>
    client.delete(`/rooms/${roomId}`),

  inviteUser: (roomId: number, username: string) =>
    client.post(`/rooms/${roomId}/invites`, { username }),

  promoteAdmin: (roomId: number, userId: number) =>
    client.post(`/rooms/${roomId}/admins/${userId}`),

  demoteAdmin: (roomId: number, userId: number) =>
    client.delete(`/rooms/${roomId}/admins/${userId}`),

  kickMember: (roomId: number, userId: number) =>
    client.post(`/rooms/${roomId}/members/${userId}/kick`),

  getBans: (roomId: number) =>
    client.get<BanResponse[]>(`/rooms/${roomId}/bans`),

  banMember: (roomId: number, userId: number) =>
    client.post(`/rooms/${roomId}/bans/${userId}`),

  unbanMember: (roomId: number, userId: number) =>
    client.delete(`/rooms/${roomId}/bans/${userId}`),
};
