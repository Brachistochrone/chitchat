import { useState, useEffect } from 'react';
import { X, Search, Crown, Shield, User, Ban, UserMinus, ShieldOff, UserPlus, Trash2 } from 'lucide-react';
import { useRoomStore } from '../../stores/useRoomStore';
import { useAuthStore } from '../../stores/useAuthStore';
import { roomsApi } from '../../api/rooms';
import type { Room, BanResponse } from '../../types/api';
import Button from '../ui/Button';
import Input from '../ui/Input';
import toast from 'react-hot-toast';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  room: Room;
}

const tabs = ['Members', 'Admins', 'Banned', 'Invitations', 'Settings'] as const;
type Tab = typeof tabs[number];

export default function ManageRoomModal({ isOpen, onClose, room }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>('Members');

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className="relative w-full max-w-2xl max-h-[80vh] flex flex-col rounded-xl bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-900">Manage: {room.name}</h2>
          <button onClick={onClose} className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex border-b border-gray-200 px-6">
          {tabs.map((tab) => (
            <button key={tab} onClick={() => setActiveTab(tab)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px
                ${activeTab === tab ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}>
              {tab}
            </button>
          ))}
        </div>
        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === 'Members' && <MembersTab room={room} />}
          {activeTab === 'Admins' && <AdminsTab room={room} />}
          {activeTab === 'Banned' && <BannedTab room={room} />}
          {activeTab === 'Invitations' && <InvitationsTab room={room} />}
          {activeTab === 'Settings' && <SettingsTab room={room} onClose={onClose} />}
        </div>
      </div>
    </div>
  );
}

function MembersTab({ room }: { room: Room }) {
  const { members, selectRoom } = useRoomStore();
  const userId = useAuthStore((s) => s.user?.id);
  const [search, setSearch] = useState('');
  const myRole = members.find((m) => m.user.id === userId)?.role;
  const isAdminOrOwner = myRole === 'OWNER' || myRole === 'ADMIN';

  const filtered = members.filter((m) => m.user.username.toLowerCase().includes(search.toLowerCase()));

  const action = async (fn: () => Promise<any>, msg: string) => {
    try { await fn(); toast.success(msg); await selectRoom(room.id); } catch { toast.error('Action failed'); }
  };

  return (
    <div className="space-y-3">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input type="text" placeholder="Search members..." value={search} onChange={(e) => setSearch(e.target.value)}
          className="w-full rounded-lg border border-gray-300 bg-white pl-9 pr-3 py-2 text-sm placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500" />
      </div>
      {filtered.map((m) => (
        <div key={m.user.id} className="flex items-center justify-between rounded-lg border border-gray-100 bg-gray-50 px-3 py-2">
          <div className="flex items-center gap-2">
            <RoleBadge role={m.role} />
            <span className="text-sm text-gray-800">{m.user.username}</span>
          </div>
          {isAdminOrOwner && m.user.id !== userId && m.role !== 'OWNER' && (
            <div className="flex gap-1">
              {myRole === 'OWNER' && m.role === 'MEMBER' && (
                <button onClick={() => action(() => roomsApi.promoteAdmin(room.id, m.user.id), 'Promoted to admin')}
                  className="rounded p-1 text-blue-600 hover:bg-blue-100" title="Make Admin"><Shield className="h-4 w-4" /></button>
              )}
              <button onClick={() => action(() => roomsApi.banMember(room.id, m.user.id), 'User banned')}
                className="rounded p-1 text-orange-600 hover:bg-orange-100" title="Ban"><Ban className="h-4 w-4" /></button>
              <button onClick={() => action(() => roomsApi.kickMember(room.id, m.user.id), 'User kicked')}
                className="rounded p-1 text-red-600 hover:bg-red-100" title="Kick"><UserMinus className="h-4 w-4" /></button>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function AdminsTab({ room }: { room: Room }) {
  const { members, selectRoom } = useRoomStore();
  const userId = useAuthStore((s) => s.user?.id);
  const isOwner = members.find((m) => m.user.id === userId)?.role === 'OWNER';
  const admins = members.filter((m) => m.role === 'OWNER' || m.role === 'ADMIN');

  const handleDemote = async (uid: number) => {
    try { await roomsApi.demoteAdmin(room.id, uid); toast.success('Demoted'); await selectRoom(room.id); }
    catch { toast.error('Failed'); }
  };

  return (
    <div className="space-y-2">
      {admins.map((m) => (
        <div key={m.user.id} className="flex items-center justify-between rounded-lg border border-gray-100 bg-gray-50 px-3 py-2">
          <div className="flex items-center gap-2">
            <RoleBadge role={m.role} />
            <span className="text-sm text-gray-800">{m.user.username}</span>
          </div>
          {isOwner && m.role === 'ADMIN' && (
            <button onClick={() => handleDemote(m.user.id)}
              className="rounded p-1 text-red-600 hover:bg-red-100" title="Remove Admin"><ShieldOff className="h-4 w-4" /></button>
          )}
        </div>
      ))}
      {admins.length === 0 && <p className="text-sm text-gray-400 text-center py-4">No admins</p>}
    </div>
  );
}

function BannedTab({ room }: { room: Room }) {
  const [bans, setBans] = useState<BanResponse[]>([]);
  const { selectRoom } = useRoomStore();

  useEffect(() => { roomsApi.getBans(room.id).then((r) => setBans(r.data)).catch(() => {}); }, [room.id]);

  const handleUnban = async (uid: number) => {
    try { await roomsApi.unbanMember(room.id, uid); toast.success('Unbanned'); setBans((b) => b.filter((x) => x.user.id !== uid)); await selectRoom(room.id); }
    catch { toast.error('Failed'); }
  };

  if (bans.length === 0) return <p className="text-sm text-gray-400 text-center py-4">No banned users</p>;

  return (
    <div className="space-y-2">
      {bans.map((b) => (
        <div key={b.user.id} className="flex items-center justify-between rounded-lg border border-gray-100 bg-gray-50 px-3 py-2">
          <div>
            <span className="text-sm text-gray-800">{b.user.username}</span>
            <span className="text-xs text-gray-400 ml-2">by {b.bannedBy.username}</span>
          </div>
          <button onClick={() => handleUnban(b.user.id)}
            className="rounded px-2 py-1 text-xs text-green-600 hover:bg-green-100">Unban</button>
        </div>
      ))}
    </div>
  );
}

function InvitationsTab({ room }: { room: Room }) {
  const [username, setUsername] = useState('');
  const [loading, setLoading] = useState(false);

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try { await roomsApi.inviteUser(room.id, username); toast.success('User invited!'); setUsername(''); }
    catch (err: any) { toast.error(err.response?.data?.message || 'Failed to invite'); }
    finally { setLoading(false); }
  };

  return (
    <form onSubmit={handleInvite} className="space-y-4">
      <Input label="Username" value={username} onChange={(e) => setUsername(e.target.value)}
        placeholder="Enter username to invite" required />
      <Button type="submit" loading={loading}>
        <UserPlus className="mr-1 h-4 w-4" /> Send Invite
      </Button>
    </form>
  );
}

function SettingsTab({ room, onClose }: { room: Room; onClose: () => void }) {
  const [name, setName] = useState(room.name);
  const [description, setDescription] = useState(room.description || '');
  const [visibility, setVisibility] = useState(room.visibility);
  const [loading, setLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const { fetchRooms, selectRoom } = useRoomStore();
  const userId = useAuthStore((s) => s.user?.id);
  const isOwner = room.owner.id === userId;

  const handleSave = async () => {
    setLoading(true);
    try { await roomsApi.updateRoom(room.id, name, description, visibility); toast.success('Room updated'); await fetchRooms(); await selectRoom(room.id); }
    catch (err: any) { toast.error(err.response?.data?.message || 'Failed'); }
    finally { setLoading(false); }
  };

  const handleDelete = async () => {
    try { await roomsApi.deleteRoom(room.id); toast.success('Room deleted'); await fetchRooms(); selectRoom(null); onClose(); }
    catch { toast.error('Failed to delete'); }
  };

  return (
    <div className="space-y-4">
      <Input label="Room Name" value={name} onChange={(e) => setName(e.target.value)} maxLength={100} />
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} maxLength={2000}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500" />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">Visibility</label>
        <div className="flex gap-4">
          <label className="flex items-center gap-2 text-sm"><input type="radio" checked={visibility === 'PUBLIC'} onChange={() => setVisibility('PUBLIC')} /> Public</label>
          <label className="flex items-center gap-2 text-sm"><input type="radio" checked={visibility === 'PRIVATE'} onChange={() => setVisibility('PRIVATE')} /> Private</label>
        </div>
      </div>
      <Button onClick={handleSave} loading={loading}>Save Changes</Button>

      {isOwner && (
        <div className="border-t border-gray-200 pt-4 mt-4">
          {!confirmDelete ? (
            <Button variant="danger" onClick={() => setConfirmDelete(true)}>
              <Trash2 className="mr-1 h-4 w-4" /> Delete Room
            </Button>
          ) : (
            <div className="flex items-center gap-2">
              <span className="text-sm text-red-600">Are you sure?</span>
              <Button variant="danger" size="sm" onClick={handleDelete}>Yes, delete</Button>
              <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(false)}>Cancel</Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function RoleBadge({ role }: { role: string }) {
  if (role === 'OWNER') return <Crown className="h-4 w-4 text-yellow-600" />;
  if (role === 'ADMIN') return <Shield className="h-4 w-4 text-blue-600" />;
  return <User className="h-4 w-4 text-gray-400" />;
}
