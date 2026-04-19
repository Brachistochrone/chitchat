import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import { authApi } from '../../api/auth';
import { useAuthStore } from '../../stores/useAuthStore';

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSwitchToForgot: () => void;
}

export default function LoginModal({ isOpen, onClose, onSwitchToForgot }: LoginModalProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const setAuth = useAuthStore((s) => s.setAuth);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await authApi.login(email, password);
      setAuth(data.token, data.user);
      toast.success('Welcome back!');
      onClose();
      navigate('/chat');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Sign In">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Email"
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input
          label="Password"
          type="password"
          placeholder="Your password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button type="submit" loading={loading} className="w-full">
          Sign In
        </Button>
        <button
          type="button"
          onClick={onSwitchToForgot}
          className="w-full text-sm text-indigo-600 hover:underline"
        >
          Forgot password?
        </button>
      </form>
    </Modal>
  );
}
