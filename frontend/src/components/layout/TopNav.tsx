import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MessageCircle, LogOut, User, ChevronDown, UserPlus, Monitor } from 'lucide-react';
import { authApi } from '../../api/auth';
import { useAuthStore } from '../../stores/useAuthStore';
import { useContactStore } from '../../stores/useContactStore';
import ProfileModal from '../profile/ProfileModal';
import SessionsModal from '../profile/SessionsModal';
import IncomingRequests from '../contact/IncomingRequests';
import Modal from '../ui/Modal';
import toast from 'react-hot-toast';

export default function TopNav() {
  const { user, clearAuth } = useAuthStore();
  const { incomingRequests, fetchIncomingRequests } = useContactStore();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [showSessions, setShowSessions] = useState(false);
  const [showRequests, setShowRequests] = useState(false);

  const handleSignOut = async () => {
    try { await authApi.logout(); } catch {}
    clearAuth();
    toast.success('Signed out');
    navigate('/');
  };

  const handleShowRequests = () => {
    fetchIncomingRequests();
    setShowRequests(true);
  };

  return (
    <>
      <nav className="flex h-14 items-center justify-between bg-gray-900 px-4 text-white">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2 font-bold text-lg">
            <MessageCircle className="h-6 w-6 text-indigo-400" />
            Chitchat
          </div>
          <div className="hidden md:flex items-center gap-1 text-sm text-gray-300">
            <button onClick={handleShowRequests}
              className="relative flex items-center gap-1.5 rounded-lg px-3 py-1.5 hover:text-white hover:bg-gray-800 transition-colors">
              <UserPlus className="h-4 w-4" /> Requests
              {incomingRequests.length > 0 && (
                <span className="absolute -top-1 -right-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
                  {incomingRequests.length}
                </span>
              )}
            </button>
            <button onClick={() => setShowSessions(true)}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 hover:text-white hover:bg-gray-800 transition-colors">
              <Monitor className="h-4 w-4" /> Sessions
            </button>
          </div>
        </div>

        <div className="relative">
          <button onClick={() => setDropdownOpen(!dropdownOpen)}
            className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-800 transition-colors">
            <User className="h-4 w-4" />
            <span>{user?.displayName || user?.username || 'User'}</span>
            <ChevronDown className="h-3 w-3" />
          </button>
          {dropdownOpen && (
            <>
              <div className="fixed inset-0 z-10" onClick={() => setDropdownOpen(false)} />
              <div className="absolute right-0 top-full z-20 mt-1 w-48 rounded-lg bg-white py-1 shadow-lg ring-1 ring-gray-200">
                <button onClick={() => { setDropdownOpen(false); setShowProfile(true); }}
                  className="flex w-full items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                  <User className="h-4 w-4" /> Profile
                </button>
                <button onClick={handleSignOut}
                  className="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50">
                  <LogOut className="h-4 w-4" /> Sign out
                </button>
              </div>
            </>
          )}
        </div>
      </nav>

      <ProfileModal isOpen={showProfile} onClose={() => setShowProfile(false)} />
      <SessionsModal isOpen={showSessions} onClose={() => setShowSessions(false)} />
      <Modal isOpen={showRequests} onClose={() => setShowRequests(false)} title="Friend Requests">
        <IncomingRequests />
      </Modal>
    </>
  );
}
