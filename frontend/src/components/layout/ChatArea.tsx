import { MessageCircle } from 'lucide-react';

export default function ChatArea() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center bg-white text-gray-400">
      <MessageCircle className="h-16 w-16 mb-4 text-gray-300" />
      <p className="text-lg font-medium">Select a room or contact to start chatting</p>
      <p className="text-sm mt-1">Your messages will appear here</p>
    </div>
  );
}
