import { useState } from 'react';
import { Globe, Lock, Crown, Shield, User, Settings } from 'lucide-react';
import { useRoomStore } from '../../stores/useRoomStore';
import { useAuthStore } from '../../stores/useAuthStore';
import ManageRoomModal from '../room/ManageRoomModal';

const presenceColors: Record<string, string> = {
  ONLINE: 'bg-green-500',
  AFK: 'bg-yellow-500',
  OFFLINE: 'bg-gray-300',
};

const roleBadges: Record<string, { icon: React.ReactNode; label: string }> = {
  OWNER: { icon: <Crown className="h-3 w-3 text-yellow-600" />, label: 'Owner' },
  ADMIN: { icon: <Shield className="h-3 w-3 text-blue-600" />, label: 'Admin' },
  MEMBER: { icon: <User className="h-3 w-3 text-gray-400" />, label: 'Member' },
};

export default function RightPanel() {
  const { rooms, selectedRoomId, members } = useRoomStore();
  const userId = useAuthStore((s) => s.user?.id);
  const [showManage, setShowManage] = useState(false);
  const room = rooms.find((r) => r.id === selectedRoomId);

  if (!room) {
    return (
      <aside className="hidden lg:flex w-72 flex-col border-l border-gray-200 bg-gray-50 items-center justify-center text-gray-400">
        <p className="text-sm">Room info and members will appear here</p>
      </aside>
    );
  }

  const myRole = members.find((m) => m.user.id === userId)?.role;
  const isAdminOrOwner = myRole === 'OWNER' || myRole === 'ADMIN';

  return (
    <aside className="hidden lg:flex w-72 flex-col border-l border-gray-200 bg-gray-50">
      <div className="border-b border-gray-200 p-4">
        <div className="flex items-center gap-2 mb-1">
          {room.visibility === 'PUBLIC' ? <Globe className="h-4 w-4 text-gray-400" /> : <Lock className="h-4 w-4 text-gray-400" />}
          <h3 className="text-sm font-semibold text-gray-900">{room.name}</h3>
        </div>
        <p className="text-xs text-gray-500">Owner: {room.owner.username} &middot; {room.memberCount} members</p>
        {isAdminOrOwner && (
          <button onClick={() => setShowManage(true)}
            className="mt-2 flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-1.5 text-xs text-gray-600 hover:bg-gray-100 transition-colors">
            <Settings className="h-3.5 w-3.5" /> Manage Room
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto p-3">
        <h4 className="mb-2 text-xs font-semibold uppercase text-gray-500">Members ({members.length})</h4>
        <div className="space-y-1">
          {members.map((m) => {
            const badge = roleBadges[m.role];
            const presence = (m as any).presence || 'OFFLINE';
            return (
              <div key={m.user.id} className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm">
                <div className="relative">
                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-gray-200 text-xs font-medium text-gray-600">
                    {m.user.username[0].toUpperCase()}
                  </div>
                  <span className={`absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-gray-50 ${presenceColors[presence]}`} />
                </div>
                <div className="min-w-0 flex-1"><span className="truncate text-gray-800">{m.user.username}</span></div>
                <div className="flex items-center gap-1 text-xs text-gray-400" title={badge.label}>{badge.icon}</div>
              </div>
            );
          })}
        </div>
      </div>

      {showManage && <ManageRoomModal isOpen={showManage} onClose={() => setShowManage(false)} room={room} />}
    </aside>
  );
}
