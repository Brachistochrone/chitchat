import { useState } from 'react';
import toast from 'react-hot-toast';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import { authApi } from '../../api/auth';

interface ForgotPasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function ForgotPasswordModal({ isOpen, onClose }: ForgotPasswordModalProps) {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await authApi.requestPasswordReset(email);
      setSent(true);
      toast.success('Reset link sent!');
    } catch {
      toast.error('Failed to send reset link');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Reset Password">
      {sent ? (
        <div className="text-center py-4">
          <p className="text-gray-600">Check your email for a reset link.</p>
          <Button variant="ghost" onClick={onClose} className="mt-4">
            Close
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Email"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Button type="submit" loading={loading} className="w-full">
            Send Reset Link
          </Button>
        </form>
      )}
    </Modal>
  );
}
