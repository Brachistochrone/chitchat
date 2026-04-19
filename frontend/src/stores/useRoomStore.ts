import { create } from 'zustand';
import type { Room, MemberResponse } from '../types/api';
import { roomsApi } from '../api/rooms';

interface RoomState {
  rooms: Room[];
  selectedRoomId: number | null;
  members: MemberResponse[];
  loading: boolean;
  fetchRooms: () => Promise<void>;
  selectRoom: (id: number | null) => Promise<void>;
  createRoom: (name: string, description: string | null, visibility: 'PUBLIC' | 'PRIVATE') => Promise<Room>;
  joinRoom: (id: number) => Promise<void>;
  leaveRoom: (id: number) => Promise<void>;
  setMembers: (members: MemberResponse[]) => void;
  updateMemberPresence: (userId: number, status: 'ONLINE' | 'AFK' | 'OFFLINE') => void;
}

export const useRoomStore = create<RoomState>((set, get) => ({
  rooms: [],
  selectedRoomId: null,
  members: [],
  loading: false,

  fetchRooms: async () => {
    set({ loading: true });
    try {
      const { data } = await roomsApi.searchPublicRooms('', 0, 100);
      set({ rooms: data.content });
    } finally {
      set({ loading: false });
    }
  },

  selectRoom: async (id) => {
    set({ selectedRoomId: id, members: [] });
    if (id) {
      try {
        const { data } = await roomsApi.getMembers(id);
        set({ members: data });
      } catch { /* ignore */ }
    }
  },

  createRoom: async (name, description, visibility) => {
    const { data } = await roomsApi.createRoom(name, description, visibility);
    set((s) => ({ rooms: [data, ...s.rooms] }));
    await get().selectRoom(data.id);
    return data;
  },

  joinRoom: async (id) => {
    await roomsApi.joinRoom(id);
    await get().fetchRooms();
  },

  leaveRoom: async (id) => {
    await roomsApi.leaveRoom(id);
    if (get().selectedRoomId === id) set({ selectedRoomId: null, members: [] });
    await get().fetchRooms();
  },

  setMembers: (members) => set({ members }),

  updateMemberPresence: (userId, status) =>
    set((s) => ({
      members: s.members.map((m) =>
        m.user.id === userId ? { ...m, presence: status } : m
      ),
    })),
}));
