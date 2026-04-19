import { create } from 'zustand';
import type { Contact } from '../types/api';
import { contactsApi } from '../api/contacts';

interface ContactState {
  friends: Contact[];
  incomingRequests: Contact[];
  selectedContactId: number | null;
  loading: boolean;
  fetchFriends: () => Promise<void>;
  fetchIncomingRequests: () => Promise<void>;
  sendRequest: (targetUsername: string, message?: string) => Promise<void>;
  acceptRequest: (requestId: number) => Promise<void>;
  declineRequest: (requestId: number) => Promise<void>;
  removeFriend: (userId: number) => Promise<void>;
  banUser: (userId: number) => Promise<void>;
  unbanUser: (userId: number) => Promise<void>;
  selectContact: (id: number | null) => void;
}

export const useContactStore = create<ContactState>((set, get) => ({
  friends: [],
  incomingRequests: [],
  selectedContactId: null,
  loading: false,

  fetchFriends: async () => {
    try {
      const { data } = await contactsApi.getFriends();
      set({ friends: data });
    } catch { /* ignore */ }
  },

  fetchIncomingRequests: async () => {
    try {
      const { data } = await contactsApi.getIncomingRequests();
      set({ incomingRequests: data });
    } catch { /* ignore */ }
  },

  sendRequest: async (targetUsername, message) => {
    await contactsApi.sendFriendRequest(targetUsername, message);
  },

  acceptRequest: async (requestId) => {
    await contactsApi.acceptFriendRequest(requestId);
    await get().fetchFriends();
    await get().fetchIncomingRequests();
  },

  declineRequest: async (requestId) => {
    await contactsApi.declineFriendRequest(requestId);
    set((s) => ({ incomingRequests: s.incomingRequests.filter((r) => r.id !== requestId) }));
  },

  removeFriend: async (userId) => {
    await contactsApi.removeFriend(userId);
    set((s) => ({ friends: s.friends.filter((f) => f.user.id !== userId) }));
  },

  banUser: async (userId) => {
    await contactsApi.banUser(userId);
    set((s) => ({ friends: s.friends.filter((f) => f.user.id !== userId) }));
  },

  unbanUser: async (userId) => {
    await contactsApi.unbanUser(userId);
  },

  selectContact: (id) => set({ selectedContactId: id }),
}));
