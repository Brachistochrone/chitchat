import { create } from 'zustand';
import type { Room } from '../types/api';

interface RoomState {
  rooms: Room[];
  selectedRoomId: number | null;
  setRooms: (rooms: Room[]) => void;
  selectRoom: (id: number | null) => void;
}

export const useRoomStore = create<RoomState>((set) => ({
  rooms: [],
  selectedRoomId: null,
  setRooms: (rooms) => set({ rooms }),
  selectRoom: (id) => set({ selectedRoomId: id }),
}));
