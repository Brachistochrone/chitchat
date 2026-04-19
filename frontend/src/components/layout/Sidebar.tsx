import { useState, useEffect } from 'react';
import { Hash, Lock, Users, Plus, Search, UserPlus, Circle } from 'lucide-react';
import { useRoomStore } from '../../stores/useRoomStore';
import { useContactStore } from '../../stores/useContactStore';
import { useUnreadStore } from '../../stores/useUnreadStore';
import { notificationsApi } from '../../api/notifications';
import CreateRoomModal from '../room/CreateRoomModal';
import FriendRequestModal from '../contact/FriendRequestModal';

export default function Sidebar() {
  const { rooms, selectedRoomId, fetchRooms, selectRoom } = useRoomStore();
  const { friends, fetchFriends, selectContact, selectedContactId } = useContactStore();
  const { unreads, reset } = useUnreadStore();
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [showAddFriend, setShowAddFriend] = useState(false);

  useEffect(() => { fetchRooms(); fetchFriends(); }, [fetchRooms, fetchFriends]);

  const filtered = rooms.filter((r) =>
    r.name.toLowerCase().includes(search.toLowerCase())
  );
  const publicRooms = filtered.filter((r) => r.visibility === 'PUBLIC');
  const privateRooms = filtered.filter((r) => r.visibility === 'PRIVATE');

  const handleSelectRoom = async (id: number) => {
    selectContact(null);
    await selectRoom(id);
    reset(`room:${id}`);
    try { await notificationsApi.markRoomRead(id); } catch { /* ignore */ }
  };

  const handleSelectContact = async (id: number) => {
    await selectRoom(null);
    selectContact(id);
    reset(`chat:${id}`);
    try { await notificationsApi.markPersonalRead(id); } catch { /* ignore */ }
  };

  return (
    <aside className="flex w-64 flex-col border-r border-gray-200 bg-gray-50">
      <div className="p-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Search rooms..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-lg border border-gray-300 bg-white pl-9 pr-3 py-2 text-sm
              placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-2">
        {publicRooms.length > 0 && (
          <RoomSection title="Public Rooms" icon={<Hash className="h-3.5 w-3.5" />}
            rooms={publicRooms} selectedId={selectedRoomId} unreads={unreads} onSelect={handleSelectRoom} />
        )}
        {privateRooms.length > 0 && (
          <RoomSection title="Private Rooms" icon={<Lock className="h-3.5 w-3.5" />}
            rooms={privateRooms} selectedId={selectedRoomId} unreads={unreads} onSelect={handleSelectRoom} />
        )}
        {filtered.length === 0 && (
          <p className="px-3 py-4 text-sm text-gray-400 text-center">No rooms found</p>
        )}
        <div className="mb-4 mt-2">
          <div className="flex items-center justify-between px-2 py-1.5">
            <h3 className="flex items-center gap-1.5 text-xs font-semibold uppercase text-gray-500">
              <Users className="h-3.5 w-3.5" /> Contacts
            </h3>
            <button onClick={() => setShowAddFriend(true)} className="text-gray-400 hover:text-indigo-600" title="Add Friend">
              <UserPlus className="h-3.5 w-3.5" />
            </button>
          </div>
          {friends.length === 0 ? (
            <p className="px-3 py-2 text-sm text-gray-400">No friends yet</p>
          ) : (
            friends.map((c) => {
              const count = unreads[`chat:${c.user.id}`] || 0;
              return (
                <button key={c.user.id} onClick={() => handleSelectContact(c.user.id)}
                  className={`flex w-full items-center justify-between rounded-lg px-3 py-1.5 text-sm transition-colors
                    ${selectedContactId === c.user.id ? 'bg-indigo-100 text-indigo-700 font-medium' : 'text-gray-700 hover:bg-gray-100'}`}>
                  <div className="flex items-center gap-2">
                    <Circle className="h-2 w-2 fill-gray-300 text-gray-300" />
                    <span className="truncate">{c.user.username}</span>
                  </div>
                  {count > 0 && (
                    <span className="ml-2 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-indigo-600 px-1.5 text-xs font-bold text-white">{count}</span>
                  )}
                </button>
              );
            })
          )}
        </div>
      </div>

      <div className="border-t border-gray-200 p-3">
        <button onClick={() => setShowCreate(true)}
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors">
          <Plus className="h-4 w-4" /> Create Room
        </button>
      </div>
      <CreateRoomModal isOpen={showCreate} onClose={() => setShowCreate(false)} />
      <FriendRequestModal isOpen={showAddFriend} onClose={() => setShowAddFriend(false)} />
    </aside>
  );
}

function RoomSection({ title, icon, rooms, selectedId, unreads, onSelect }: {
  title: string; icon: React.ReactNode;
  rooms: { id: number; name: string; memberCount: number }[];
  selectedId: number | null; unreads: Record<string, number>;
  onSelect: (id: number) => void;
}) {
  return (
    <div className="mb-4">
      <h3 className="flex items-center gap-1.5 px-2 py-1.5 text-xs font-semibold uppercase text-gray-500">
        {icon} {title}
      </h3>
      {rooms.map((room) => {
        const count = unreads[`room:${room.id}`] || 0;
        return (
          <button key={room.id} onClick={() => onSelect(room.id)}
            className={`flex w-full items-center justify-between rounded-lg px-3 py-1.5 text-sm transition-colors
              ${selectedId === room.id ? 'bg-indigo-100 text-indigo-700 font-medium' : 'text-gray-700 hover:bg-gray-100'}`}>
            <span className="truncate">{room.name}</span>
            {count > 0 && (
              <span className="ml-2 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-indigo-600 px-1.5 text-xs font-bold text-white">
                {count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
