import { useEffect, useRef } from 'react';
import type { StompSubscription } from '@stomp/stompjs';
import toast from 'react-hot-toast';
import TopNav from '../components/layout/TopNav';
import Sidebar from '../components/layout/Sidebar';
import ChatArea from '../components/layout/ChatArea';
import RightPanel from '../components/layout/RightPanel';
import { useWebSocket } from '../hooks/useWebSocket';
import { useRoomStore } from '../stores/useRoomStore';
import { useContactStore } from '../stores/useContactStore';
import { useMessageStore } from '../stores/useMessageStore';
import { useUnreadStore } from '../stores/useUnreadStore';
import type { ChatMessageEvent, PresenceUpdate } from '../types/api';
import { wsService } from '../api/websocket';

export default function ChatPage() {
  useWebSocket();
  const { selectedRoomId, updateMemberPresence } = useRoomStore();
  const { fetchIncomingRequests } = useContactStore();
  const { addMessage, updateMessage, removeMessage } = useMessageStore();
  const { fetchUnreads } = useUnreadStore();
  const subsRef = useRef<StompSubscription[]>([]);

  useEffect(() => { fetchUnreads(); fetchIncomingRequests(); }, [fetchUnreads, fetchIncomingRequests]);

  // Personal messages + notifications subscription
  useEffect(() => {
    if (!wsService.connected) return;
    const personalSub = wsService.subscribe('/user/queue/messages', (event: ChatMessageEvent) => {
      if (event.eventType === 'CREATED') {
        addMessage({
          id: event.messageId, chatType: event.chatType,
          sender: { id: event.senderId, username: '', displayName: null, createdAt: '' },
          content: event.content, replyTo: null, attachments: [],
          editedAt: null, createdAt: event.createdAt,
        });
      }
    });
    const notifSub = wsService.subscribe('/user/queue/notifications', (event: any) => {
      if (event.type === 'FRIEND_REQUEST') {
        toast('New friend request!', { icon: '👋' });
        fetchIncomingRequests();
      }
    });
    return () => {
      personalSub?.unsubscribe();
      notifSub?.unsubscribe();
    };
  }, [addMessage, fetchIncomingRequests]);

  // Room subscriptions
  useEffect(() => {
    subsRef.current.forEach((s) => s.unsubscribe());
    subsRef.current = [];
    if (!selectedRoomId || !wsService.connected) return;

    const msgSub = wsService.subscribe(`/topic/rooms/${selectedRoomId}`, (event: ChatMessageEvent) => {
      if (event.eventType === 'CREATED') {
        addMessage({
          id: event.messageId, chatType: event.chatType,
          sender: { id: event.senderId, username: '', displayName: null, createdAt: '' },
          content: event.content, replyTo: null, attachments: [],
          editedAt: null, createdAt: event.createdAt,
        });
      } else if (event.eventType === 'EDITED') {
        updateMessage({
          id: event.messageId, chatType: event.chatType,
          sender: { id: event.senderId, username: '', displayName: null, createdAt: '' },
          content: event.content, replyTo: null, attachments: [],
          editedAt: event.createdAt, createdAt: event.createdAt,
        });
      } else if (event.eventType === 'DELETED') {
        removeMessage(event.messageId);
      }
    });
    const presSub = wsService.subscribe(`/topic/rooms/${selectedRoomId}/presence`, (event: PresenceUpdate) => {
      updateMemberPresence(event.userId, event.status);
    });

    if (msgSub) subsRef.current.push(msgSub);
    if (presSub) subsRef.current.push(presSub);

    return () => { subsRef.current.forEach((s) => s.unsubscribe()); subsRef.current = []; };
  }, [selectedRoomId, addMessage, updateMessage, removeMessage, updateMemberPresence]);

  return (
    <div className="flex h-screen flex-col">
      <TopNav />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <ChatArea />
        <RightPanel />
      </div>
    </div>
  );
}
