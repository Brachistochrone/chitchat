import { useEffect, useState } from 'react';
import { MessageCircle, Send } from 'lucide-react';
import { useRoomStore } from '../../stores/useRoomStore';
import { useContactStore } from '../../stores/useContactStore';
import { useMessageStore } from '../../stores/useMessageStore';
import { messagesApi } from '../../api/messages';
import { wsService } from '../../api/websocket';
import RoomHeader from '../chat/RoomHeader';
import MessageList from '../chat/MessageList';
import MessageInput from '../chat/MessageInput';

export default function ChatArea() {
  const { selectedRoomId, rooms } = useRoomStore();
  const { selectedContactId, friends } = useContactStore();
  const { fetchMessages, clearMessages } = useMessageStore();

  const room = rooms.find((r) => r.id === selectedRoomId) ?? null;
  const contact = friends.find((c) => c.user.id === selectedContactId) ?? null;

  useEffect(() => {
    clearMessages();
    if (selectedRoomId) fetchMessages(selectedRoomId);
  }, [selectedRoomId, clearMessages, fetchMessages]);

  useEffect(() => {
    if (selectedContactId) {
      clearMessages();
      messagesApi.getPersonalMessages(selectedContactId).then(({ data }) => {
        useMessageStore.setState({ messages: data, hasMore: data.length === 50 });
      }).catch(() => {});
    }
  }, [selectedContactId, clearMessages]);

  if (!room && !contact) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-white text-gray-400">
        <MessageCircle className="h-16 w-16 mb-4 text-gray-300" />
        <p className="text-lg font-medium">Select a room or contact to start chatting</p>
        <p className="text-sm mt-1">Your messages will appear here</p>
      </div>
    );
  }

  if (contact) {
    return (
      <div className="flex flex-1 flex-col bg-white">
        <div className="flex items-center gap-3 border-b border-gray-200 bg-white px-4 py-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-sm font-medium text-indigo-600">
            {contact.user.username[0].toUpperCase()}
          </div>
          <h2 className="text-sm font-semibold text-gray-900">{contact.user.username}</h2>
        </div>
        <MessageList roomId={-1} />
        <PersonalInput userId={contact.user.id} />
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col bg-white">
      <RoomHeader room={room!} />
      <MessageList roomId={room!.id} />
      <MessageInput roomId={room!.id} />
    </div>
  );
}

function PersonalInput({ userId }: { userId: number }) {
  const [content, setContent] = useState('');

  const handleSend = () => {
    const text = content.trim();
    if (!text) return;
    wsService.send(`/app/chats/${userId}/send`, { content: text });
    setContent('');
  };

  return (
    <div className="border-t border-gray-200 bg-white px-4 py-3 flex gap-2">
      <textarea value={content} onChange={(e) => setContent(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
        placeholder="Type a message..." rows={1}
        className="flex-1 resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        style={{ maxHeight: '120px' }}
        onInput={(e) => { const el = e.currentTarget; el.style.height = 'auto'; el.style.height = Math.min(el.scrollHeight, 120) + 'px'; }} />
      <button onClick={handleSend} disabled={!content.trim()}
        className="rounded-lg bg-indigo-600 p-2 text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
        <Send className="h-5 w-5" />
      </button>
    </div>
  );
}
