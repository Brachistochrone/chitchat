import { Reply, FileText, Download } from 'lucide-react';
import { format } from 'date-fns';
import type { Message } from '../../types/api';
import { useMessageStore } from '../../stores/useMessageStore';
import { attachmentsApi } from '../../api/attachments';

interface MessageBubbleProps {
  message: Message;
}

export default function MessageBubble({ message }: MessageBubbleProps) {
  const setReplyTo = useMessageStore((s) => s.setReplyTo);
  const isDeleted = message.content === null && message.attachments.length === 0;

  return (
    <div className="group px-4 py-1.5 hover:bg-gray-50 transition-colors">
      {message.replyTo && (
        <div className="mb-1 ml-8 flex items-center gap-1 text-xs text-gray-400">
          <Reply className="h-3 w-3" />
          <span className="font-medium">{message.replyTo.sender.username}:</span>
          <span className="truncate max-w-xs">
            {message.replyTo.content || '[message deleted]'}
          </span>
        </div>
      )}

      <div className="flex items-start gap-2">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-sm font-medium text-indigo-600">
          {message.sender.username[0].toUpperCase()}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-baseline gap-2">
            <span className="text-sm font-semibold text-gray-900">
              {message.sender.username}
            </span>
            <span className="text-xs text-gray-400">
              {format(new Date(message.createdAt), 'HH:mm')}
            </span>
            {message.editedAt && (
              <span className="text-xs text-gray-400 italic">(edited)</span>
            )}
          </div>

          {isDeleted ? (
            <p className="text-sm italic text-gray-400">[message deleted]</p>
          ) : (
            <>
              {message.content && (
                <p className="text-sm text-gray-700 whitespace-pre-wrap break-words">
                  {message.content}
                </p>
              )}
              {message.attachments.map((att) => (
                <div key={att.id} className="mt-1">
                  {att.mimeType?.startsWith('image/') ? (
                    <img
                      src={attachmentsApi.getDownloadUrl(att.id)}
                      alt={att.originalFilename}
                      className="max-h-60 rounded-lg border border-gray-200"
                    />
                  ) : (
                    <a
                      href={attachmentsApi.getDownloadUrl(att.id)}
                      className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-indigo-600 hover:bg-gray-100"
                      download
                    >
                      <FileText className="h-4 w-4" />
                      {att.originalFilename}
                      <Download className="h-3 w-3" />
                    </a>
                  )}
                </div>
              ))}
            </>
          )}
        </div>

        {!isDeleted && (
          <button
            onClick={() => setReplyTo(message)}
            className="invisible group-hover:visible rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600"
            title="Reply"
          >
            <Reply className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
}
