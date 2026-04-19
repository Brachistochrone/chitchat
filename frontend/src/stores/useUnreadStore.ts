import { create } from 'zustand';
import { notificationsApi } from '../api/notifications';

interface UnreadState {
  unreads: Record<string, number>;
  fetchUnreads: () => Promise<void>;
  increment: (key: string) => void;
  reset: (key: string) => void;
  getCount: (key: string) => number;
}

export const useUnreadStore = create<UnreadState>((set, get) => ({
  unreads: {},

  fetchUnreads: async () => {
    try {
      const { data } = await notificationsApi.getUnreadCounts();
      const map: Record<string, number> = {};
      data.forEach((u) => {
        if (u.roomId) map[`room:${u.roomId}`] = u.count;
        if (u.chatUserId) map[`chat:${u.chatUserId}`] = u.count;
      });
      set({ unreads: map });
    } catch { /* ignore */ }
  },

  increment: (key) =>
    set((s) => ({ unreads: { ...s.unreads, [key]: (s.unreads[key] || 0) + 1 } })),

  reset: (key) =>
    set((s) => ({ unreads: { ...s.unreads, [key]: 0 } })),

  getCount: (key) => get().unreads[key] || 0,
}));
