import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import { useAuthStore } from '../../stores/useAuthStore';
import { usersApi } from '../../api/users';

interface Props { isOpen: boolean; onClose: () => void; }

export default function ProfileModal({ isOpen, onClose }: Props) {
  const { user, setAuth, clearAuth, token } = useAuthStore();
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [saving, setSaving] = useState(false);

  const handleSaveProfile = async () => {
    setSaving(true);
    try {
      const { data } = await usersApi.updateProfile(displayName);
      if (token) setAuth(token, data);
      toast.success('Profile updated');
    } catch { toast.error('Failed to update'); }
    finally { setSaving(false); }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await usersApi.changePassword(currentPassword, newPassword);
      toast.success('Password changed');
      setCurrentPassword(''); setNewPassword('');
    } catch (err: any) { toast.error(err.response?.data?.message || 'Failed'); }
  };

  const handleDeleteAccount = async () => {
    try {
      await usersApi.deleteAccount();
      clearAuth();
      toast.success('Account deleted');
      navigate('/');
    } catch { toast.error('Failed to delete account'); }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Profile">
      <div className="space-y-6">
        <div className="space-y-3">
          <Input label="Display Name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          <Input label="Username" value={user?.username || ''} disabled />
          <Button onClick={handleSaveProfile} loading={saving} size="sm">Save Profile</Button>
        </div>

        <div className="border-t border-gray-200 pt-4">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">Change Password</h3>
          <form onSubmit={handleChangePassword} className="space-y-3">
            <Input label="Current Password" type="password" value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)} required />
            <Input label="New Password" type="password" value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)} required />
            <Button type="submit" size="sm" variant="secondary">Change Password</Button>
          </form>
        </div>

        <div className="border-t border-gray-200 pt-4">
          {!confirmDelete ? (
            <Button variant="danger" size="sm" onClick={() => setConfirmDelete(true)}>Delete Account</Button>
          ) : (
            <div className="space-y-2">
              <p className="text-sm text-red-600">This action is irreversible. All your data will be deleted.</p>
              <div className="flex gap-2">
                <Button variant="danger" size="sm" onClick={handleDeleteAccount}>Yes, delete my account</Button>
                <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(false)}>Cancel</Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}
