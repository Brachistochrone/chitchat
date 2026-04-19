import { useEffect } from 'react';
import { Check, X, UserPlus } from 'lucide-react';
import { useContactStore } from '../../stores/useContactStore';
import toast from 'react-hot-toast';
import { format } from 'date-fns';

export default function IncomingRequests() {
  const { incomingRequests, fetchIncomingRequests, acceptRequest, declineRequest } = useContactStore();

  useEffect(() => { fetchIncomingRequests(); }, [fetchIncomingRequests]);

  const handleAccept = async (id: number) => {
    try {
      await acceptRequest(id);
      toast.success('Friend request accepted!');
    } catch { toast.error('Failed to accept'); }
  };

  const handleDecline = async (id: number) => {
    try {
      await declineRequest(id);
      toast.success('Request declined');
    } catch { toast.error('Failed to decline'); }
  };

  if (incomingRequests.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center text-gray-400">
        <UserPlus className="h-12 w-12 mb-3 text-gray-300" />
        <p className="text-sm">No pending requests</p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4">
      <h2 className="text-sm font-semibold text-gray-900 mb-3">Incoming Friend Requests</h2>
      <div className="space-y-2">
        {incomingRequests.map((req) => (
          <div key={req.id} className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-3">
            <div>
              <p className="text-sm font-medium text-gray-900">{req.user.username}</p>
              {req.message && <p className="text-xs text-gray-500 mt-0.5">{req.message}</p>}
              <p className="text-xs text-gray-400 mt-0.5">{format(new Date(req.createdAt), 'MMM d, yyyy')}</p>
            </div>
            <div className="flex gap-2">
              <button onClick={() => handleAccept(req.id)}
                className="rounded-lg bg-green-100 p-2 text-green-600 hover:bg-green-200 transition-colors">
                <Check className="h-4 w-4" />
              </button>
              <button onClick={() => handleDecline(req.id)}
                className="rounded-lg bg-red-100 p-2 text-red-600 hover:bg-red-200 transition-colors">
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
