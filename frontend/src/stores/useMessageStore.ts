import { create } from 'zustand';
import type { Message } from '../types/api';

interface MessageState {
  messages: Message[];
  loading: boolean;
  setMessages: (messages: Message[]) => void;
  addMessage: (message: Message) => void;
}

export const useMessageStore = create<MessageState>((set) => ({
  messages: [],
  loading: false,
  setMessages: (messages) => set({ messages }),
  addMessage: (message) => set((state) => ({ messages: [...state.messages, message] })),
}));
