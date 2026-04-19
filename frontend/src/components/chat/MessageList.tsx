import { useEffect, useRef, useState, useCallback } from 'react';
import { Loader2, ArrowDown } from 'lucide-react';
import { useMessageStore } from '../../stores/useMessageStore';
import MessageBubble from './MessageBubble';

interface MessageListProps {
  roomId: number;
}

export default function MessageList({ roomId }: MessageListProps) {
  const { messages, loading, hasMore, fetchMessages } = useMessageStore();
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [isAtBottom, setIsAtBottom] = useState(true);
  const [newCount, setNewCount] = useState(0);
  const prevLengthRef = useRef(messages.length);

  useEffect(() => {
    if (messages.length > prevLengthRef.current && !isAtBottom) {
      setNewCount((c) => c + (messages.length - prevLengthRef.current));
    }
    prevLengthRef.current = messages.length;
  }, [messages.length, isAtBottom]);

  useEffect(() => {
    if (isAtBottom) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
      setNewCount(0);
    }
  }, [messages.length, isAtBottom]);

  const handleScroll = useCallback(() => {
    const el = containerRef.current;
    if (!el) return;
    const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 50;
    setIsAtBottom(atBottom);
    if (atBottom) setNewCount(0);

    if (el.scrollTop < 100 && hasMore && !loading) {
      const oldest = messages[0];
      if (oldest) {
        const prevHeight = el.scrollHeight;
        fetchMessages(roomId, oldest.createdAt).then(() => {
          requestAnimationFrame(() => {
            el.scrollTop = el.scrollHeight - prevHeight;
          });
        });
      }
    }
  }, [hasMore, loading, messages, roomId, fetchMessages]);

  const scrollToBottom = () => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    setNewCount(0);
  };

  return (
    <div className="relative flex-1 overflow-hidden">
      <div
        ref={containerRef}
        onScroll={handleScroll}
        className="h-full overflow-y-auto"
      >
        {loading && messages.length > 0 && (
          <div className="flex justify-center py-2">
            <Loader2 className="h-5 w-5 animate-spin text-gray-400" />
          </div>
        )}
        {messages.map((msg) => (
          <MessageBubble key={msg.id} message={msg} />
        ))}
        <div ref={bottomRef} />
      </div>

      {newCount > 0 && (
        <button
          onClick={scrollToBottom}
          className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-1.5
            rounded-full bg-indigo-600 px-4 py-1.5 text-sm text-white shadow-lg
            hover:bg-indigo-700 transition-colors"
        >
          {newCount} new message{newCount > 1 ? 's' : ''}
          <ArrowDown className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}
