import { Globe, Lock, LogOut, LogIn } from 'lucide-react';
import type { Room } from '../../types/api';
import { useRoomStore } from '../../stores/useRoomStore';
import { useAuthStore } from '../../stores/useAuthStore';
import Button from '../ui/Button';
import toast from 'react-hot-toast';

interface RoomHeaderProps {
  room: Room;
}

export default function RoomHeader({ room }: RoomHeaderProps) {
  const { members, joinRoom, leaveRoom } = useRoomStore();
  const userId = useAuthStore((s) => s.user?.id);
  const isMember = members.some((m) => m.user.id === userId);
  const isOwner = room.owner.id === userId;

  const handleJoin = async () => {
    try {
      await joinRoom(room.id);
      toast.success('Joined room');
    } catch {
      toast.error('Failed to join');
    }
  };

  const handleLeave = async () => {
    try {
      await leaveRoom(room.id);
      toast.success('Left room');
    } catch {
      toast.error('Failed to leave');
    }
  };

  return (
    <div className="flex items-center justify-between border-b border-gray-200 bg-white px-4 py-3">
      <div className="flex items-center gap-3">
        {room.visibility === 'PUBLIC' ? (
          <Globe className="h-5 w-5 text-gray-400" />
        ) : (
          <Lock className="h-5 w-5 text-gray-400" />
        )}
        <div>
          <h2 className="text-sm font-semibold text-gray-900">{room.name}</h2>
          {room.description && (
            <p className="text-xs text-gray-500 truncate max-w-md">{room.description}</p>
          )}
        </div>
      </div>
      <div>
        {!isMember && room.visibility === 'PUBLIC' && (
          <Button size="sm" onClick={handleJoin}>
            <LogIn className="mr-1 h-4 w-4" /> Join
          </Button>
        )}
        {isMember && !isOwner && (
          <Button size="sm" variant="ghost" onClick={handleLeave}>
            <LogOut className="mr-1 h-4 w-4" /> Leave
          </Button>
        )}
      </div>
    </div>
  );
}
