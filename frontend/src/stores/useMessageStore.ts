import { create } from 'zustand';
import type { Message } from '../types/api';
import { messagesApi } from '../api/messages';

interface MessageState {
  messages: Message[];
  loading: boolean;
  hasMore: boolean;
  replyTo: Message | null;
  fetchMessages: (roomId: number, before?: string) => Promise<void>;
  addMessage: (message: Message) => void;
  updateMessage: (message: Message) => void;
  removeMessage: (messageId: number) => void;
  setReplyTo: (message: Message | null) => void;
  clearMessages: () => void;
}

export const useMessageStore = create<MessageState>((set) => ({
  messages: [],
  loading: false,
  hasMore: true,
  replyTo: null,

  fetchMessages: async (roomId, before) => {
    set({ loading: true });
    try {
      const { data } = await messagesApi.getRoomMessages(roomId, before, 50);
      if (before) {
        set((s) => ({ messages: [...data, ...s.messages], hasMore: data.length === 50 }));
      } else {
        set({ messages: data, hasMore: data.length === 50 });
      }
    } finally {
      set({ loading: false });
    }
  },

  addMessage: (message) =>
    set((s) => ({ messages: [...s.messages, message] })),

  updateMessage: (message) =>
    set((s) => ({
      messages: s.messages.map((m) => (m.id === message.id ? message : m)),
    })),

  removeMessage: (messageId) =>
    set((s) => ({
      messages: s.messages.map((m) =>
        m.id === messageId ? { ...m, content: null, attachments: [] } : m
      ),
    })),

  setReplyTo: (message) => set({ replyTo: message }),

  clearMessages: () => set({ messages: [], hasMore: true, replyTo: null }),
}));
