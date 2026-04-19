import { useState, useEffect } from 'react';
import { Monitor, Trash2 } from 'lucide-react';
import { format } from 'date-fns';
import Modal from '../ui/Modal';
import { sessionsApi } from '../../api/sessions';
import type { Session } from '../../types/api';
import toast from 'react-hot-toast';

interface Props { isOpen: boolean; onClose: () => void; }

export default function SessionsModal({ isOpen, onClose }: Props) {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      sessionsApi.getActiveSessions()
        .then((r) => setSessions(r.data))
        .catch(() => toast.error('Failed to load sessions'))
        .finally(() => setLoading(false));
    }
  }, [isOpen]);

  const handleRevoke = async (id: number) => {
    try {
      await sessionsApi.revokeSession(id);
      setSessions((s) => s.filter((x) => x.id !== id));
      toast.success('Session revoked');
    } catch { toast.error('Failed to revoke'); }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Active Sessions">
      {loading ? (
        <p className="text-sm text-gray-400 text-center py-4">Loading...</p>
      ) : (
        <div className="space-y-2 max-h-80 overflow-y-auto">
          {sessions.map((s) => (
            <div key={s.id} className="flex items-center justify-between rounded-lg border border-gray-100 bg-gray-50 px-3 py-2">
              <div className="flex items-center gap-2">
                <Monitor className="h-4 w-4 text-gray-400" />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-gray-800">{s.browser || 'Unknown browser'}</span>
                    {s.current && <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">Current</span>}
                  </div>
                  <p className="text-xs text-gray-400">{s.ipAddress} &middot; {format(new Date(s.lastSeenAt), 'MMM d, HH:mm')}</p>
                </div>
              </div>
              {!s.current && (
                <button onClick={() => handleRevoke(s.id)}
                  className="rounded p-1 text-red-500 hover:bg-red-100" title="Revoke">
                  <Trash2 className="h-4 w-4" />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </Modal>
  );
}
