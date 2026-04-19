import { useState, useRef, useCallback } from 'react';
import { Send, Smile, Paperclip, X, FileText } from 'lucide-react';
import data from '@emoji-mart/data';
import Picker from '@emoji-mart/react';
import { useMessageStore } from '../../stores/useMessageStore';
import { messagesApi } from '../../api/messages';
import { attachmentsApi } from '../../api/attachments';
import type { Attachment } from '../../types/api';
import toast from 'react-hot-toast';

interface MessageInputProps {
  roomId: number;
}

export default function MessageInput({ roomId }: MessageInputProps) {
  const [content, setContent] = useState('');
  const [showEmoji, setShowEmoji] = useState(false);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [uploading, setUploading] = useState(false);
  const { replyTo, setReplyTo } = useMessageStore();
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleSend = useCallback(async () => {
    const text = content.trim();
    if (!text && attachments.length === 0) return;
    try {
      await messagesApi.sendRoomMessage(
        roomId,
        text,
        replyTo?.id,
        attachments.map((a) => a.id)
      );
      setContent('');
      setReplyTo(null);
      setAttachments([]);
    } catch {
      toast.error('Failed to send message');
    }
  }, [content, roomId, replyTo, attachments, setReplyTo]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const { data } = await attachmentsApi.upload(file);
      setAttachments((prev) => [...prev, data]);
    } catch {
      toast.error('Failed to upload file');
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const handleEmojiSelect = (emoji: any) => {
    setContent((prev) => prev + emoji.native);
    setShowEmoji(false);
    textareaRef.current?.focus();
  };

  const removeAttachment = (id: number) => {
    setAttachments((prev) => prev.filter((a) => a.id !== id));
  };

  return (
    <div className="border-t border-gray-200 bg-white">
      {replyTo && (
        <div className="flex items-center gap-2 border-b border-gray-100 bg-gray-50 px-4 py-2 text-sm">
          <span className="text-gray-400">Replying to</span>
          <span className="font-medium text-gray-700">{replyTo.sender.username}</span>
          <span className="truncate text-gray-500 max-w-xs">{replyTo.content}</span>
          <button onClick={() => setReplyTo(null)} className="ml-auto text-gray-400 hover:text-gray-600">
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {attachments.length > 0 && (
        <div className="flex flex-wrap gap-2 border-b border-gray-100 px-4 py-2">
          {attachments.map((att) => (
            <div key={att.id} className="flex items-center gap-1.5 rounded-lg bg-gray-100 px-2 py-1 text-sm">
              <FileText className="h-3.5 w-3.5 text-gray-500" />
              <span className="max-w-[120px] truncate">{att.originalFilename}</span>
              <button onClick={() => removeAttachment(att.id)} className="text-gray-400 hover:text-gray-600">
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="relative flex items-end gap-2 px-4 py-3">
        <div className="relative">
          <button
            onClick={() => setShowEmoji(!showEmoji)}
            className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
          >
            <Smile className="h-5 w-5" />
          </button>
          {showEmoji && (
            <div className="absolute bottom-12 left-0 z-50">
              <Picker data={data} onEmojiSelect={handleEmojiSelect} theme="light" />
            </div>
          )}
        </div>

        <button
          onClick={() => fileRef.current?.click()}
          disabled={uploading}
          className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 disabled:opacity-50"
        >
          <Paperclip className="h-5 w-5" />
        </button>
        <input ref={fileRef} type="file" className="hidden" onChange={handleFileChange} />

        <textarea
          ref={textareaRef}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Type a message..."
          rows={1}
          className="flex-1 resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm
            placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1
            focus:ring-indigo-500"
          style={{ maxHeight: '120px' }}
          onInput={(e) => {
            const el = e.currentTarget;
            el.style.height = 'auto';
            el.style.height = Math.min(el.scrollHeight, 120) + 'px';
          }}
        />

        <button
          onClick={handleSend}
          disabled={!content.trim() && attachments.length === 0}
          className="rounded-lg bg-indigo-600 p-2 text-white hover:bg-indigo-700
            disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <Send className="h-5 w-5" />
        </button>
      </div>
    </div>
  );
}
