import { useState } from 'react';
import { Reply, FileText, Download, Pencil, Trash2, Check, X } from 'lucide-react';
import { format } from 'date-fns';
import type { Message } from '../../types/api';
import { useMessageStore } from '../../stores/useMessageStore';
import { useAuthStore } from '../../stores/useAuthStore';
import { messagesApi } from '../../api/messages';
import { attachmentsApi } from '../../api/attachments';
import toast from 'react-hot-toast';

interface Props { message: Message; userRole?: string; }

export default function MessageBubble({ message, userRole }: Props) {
  const setReplyTo = useMessageStore((s) => s.setReplyTo);
  const updateMessage = useMessageStore((s) => s.updateMessage);
  const removeMessage = useMessageStore((s) => s.removeMessage);
  const userId = useAuthStore((s) => s.user?.id);
  const [editing, setEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content || '');
  const isDeleted = message.content === null && message.attachments.length === 0;
  const isOwn = message.sender.id === userId;
  const canDelete = isOwn || userRole === 'OWNER' || userRole === 'ADMIN';

  const handleEdit = async () => {
    try {
      const { data } = await messagesApi.editMessage(message.id, editContent);
      updateMessage(data);
      setEditing(false);
    } catch { toast.error('Failed to edit'); }
  };

  const handleDelete = async () => {
    try { await messagesApi.deleteMessage(message.id); removeMessage(message.id); }
    catch { toast.error('Failed to delete'); }
  };

  return (
    <div className="group px-4 py-1.5 hover:bg-gray-50 transition-colors">
      {message.replyTo && (
        <div className="mb-1 ml-8 flex items-center gap-1 text-xs text-gray-400">
          <Reply className="h-3 w-3" />
          <span className="font-medium">{message.replyTo.sender.username}:</span>
          <span className="truncate max-w-xs">{message.replyTo.content || '[message deleted]'}</span>
        </div>
      )}
      <div className="flex items-start gap-2">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-sm font-medium text-indigo-600">
          {message.sender.username[0].toUpperCase()}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-baseline gap-2">
            <span className="text-sm font-semibold text-gray-900">{message.sender.username}</span>
            <span className="text-xs text-gray-400">{format(new Date(message.createdAt), 'HH:mm')}</span>
            {message.editedAt && <span className="text-xs text-gray-400 italic">(edited)</span>}
          </div>
          {isDeleted ? (
            <p className="text-sm italic text-gray-400">[message deleted]</p>
          ) : editing ? (
            <div className="mt-1 flex items-center gap-2">
              <input value={editContent} onChange={(e) => setEditContent(e.target.value)}
                className="flex-1 rounded border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none"
                onKeyDown={(e) => { if (e.key === 'Enter') handleEdit(); if (e.key === 'Escape') setEditing(false); }} autoFocus />
              <button onClick={handleEdit} className="text-green-600 hover:text-green-700"><Check className="h-4 w-4" /></button>
              <button onClick={() => setEditing(false)} className="text-gray-400 hover:text-gray-600"><X className="h-4 w-4" /></button>
            </div>
          ) : (
            <>
              {message.content && <p className="text-sm text-gray-700 whitespace-pre-wrap break-words">{message.content}</p>}
              {message.attachments.map((att) => (
                <div key={att.id} className="mt-1">
                  {att.mimeType?.startsWith('image/') ? (
                    <img src={attachmentsApi.getDownloadUrl(att.id)} alt={att.originalFilename} className="max-h-60 rounded-lg border border-gray-200" />
                  ) : (
                    <a href={attachmentsApi.getDownloadUrl(att.id)} download
                      className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-indigo-600 hover:bg-gray-100">
                      <FileText className="h-4 w-4" />{att.originalFilename}<Download className="h-3 w-3" />
                    </a>
                  )}
                </div>
              ))}
            </>
          )}
        </div>
        {!isDeleted && !editing && (
          <div className="invisible group-hover:visible flex gap-0.5">
            <button onClick={() => setReplyTo(message)} className="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600" title="Reply"><Reply className="h-4 w-4" /></button>
            {isOwn && <button onClick={() => { setEditContent(message.content || ''); setEditing(true); }} className="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600" title="Edit"><Pencil className="h-4 w-4" /></button>}
            {canDelete && <button onClick={handleDelete} className="rounded p-1 text-gray-400 hover:bg-red-100 hover:text-red-600" title="Delete"><Trash2 className="h-4 w-4" /></button>}
          </div>
        )}
      </div>
    </div>
  );
}
