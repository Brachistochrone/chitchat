import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MessageCircle, LogOut, User, ChevronDown } from 'lucide-react';
import { authApi } from '../../api/auth';
import { useAuthStore } from '../../stores/useAuthStore';
import toast from 'react-hot-toast';

export default function TopNav() {
  const { user, clearAuth } = useAuthStore();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const handleSignOut = async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore
    }
    clearAuth();
    toast.success('Signed out');
    navigate('/');
  };

  return (
    <nav className="flex h-14 items-center justify-between bg-gray-900 px-4 text-white">
      <div className="flex items-center gap-6">
        <div className="flex items-center gap-2 font-bold text-lg">
          <MessageCircle className="h-6 w-6 text-indigo-400" />
          Chitchat
        </div>
        <div className="hidden md:flex items-center gap-4 text-sm text-gray-300">
          <button className="hover:text-white transition-colors">Public Rooms</button>
          <button className="hover:text-white transition-colors">Private Rooms</button>
          <button className="hover:text-white transition-colors">Contacts</button>
          <button className="hover:text-white transition-colors">Sessions</button>
        </div>
      </div>

      <div className="relative">
        <button
          onClick={() => setDropdownOpen(!dropdownOpen)}
          className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-800 transition-colors"
        >
          <User className="h-4 w-4" />
          <span>{user?.displayName || user?.username || 'User'}</span>
          <ChevronDown className="h-3 w-3" />
        </button>

        {dropdownOpen && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setDropdownOpen(false)} />
            <div className="absolute right-0 top-full z-20 mt-1 w-48 rounded-lg bg-white py-1 shadow-lg ring-1 ring-gray-200">
              <button
                onClick={() => { setDropdownOpen(false); }}
                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                <User className="h-4 w-4" />
                Profile
              </button>
              <button
                onClick={handleSignOut}
                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </button>
            </div>
          </>
        )}
      </div>
    </nav>
  );
}
