import { useEffect } from 'react';
import { MessageCircle } from 'lucide-react';
import { useRoomStore } from '../../stores/useRoomStore';
import { useMessageStore } from '../../stores/useMessageStore';
import RoomHeader from '../chat/RoomHeader';
import MessageList from '../chat/MessageList';
import MessageInput from '../chat/MessageInput';

export default function ChatArea() {
  const { selectedRoomId, rooms } = useRoomStore();
  const { fetchMessages, clearMessages } = useMessageStore();

  const room = rooms.find((r) => r.id === selectedRoomId) ?? null;

  useEffect(() => {
    if (selectedRoomId) {
      clearMessages();
      fetchMessages(selectedRoomId);
    }
  }, [selectedRoomId, clearMessages, fetchMessages]);

  if (!room) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-white text-gray-400">
        <MessageCircle className="h-16 w-16 mb-4 text-gray-300" />
        <p className="text-lg font-medium">Select a room or contact to start chatting</p>
        <p className="text-sm mt-1">Your messages will appear here</p>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col bg-white">
      <RoomHeader room={room} />
      <MessageList roomId={room.id} />
      <MessageInput roomId={room.id} />
    </div>
  );
}
