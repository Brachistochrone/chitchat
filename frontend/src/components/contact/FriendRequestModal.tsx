import { useState } from 'react';
import toast from 'react-hot-toast';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import { useContactStore } from '../../stores/useContactStore';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export default function FriendRequestModal({ isOpen, onClose }: Props) {
  const [username, setUsername] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const sendRequest = useContactStore((s) => s.sendRequest);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await sendRequest(username, message || undefined);
      toast.success('Friend request sent!');
      setUsername('');
      setMessage('');
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to send request');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Add Friend">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input label="Username" value={username} onChange={(e) => setUsername(e.target.value)}
          placeholder="Enter username" required />
        <Input label="Message (optional)" value={message} onChange={(e) => setMessage(e.target.value)}
          placeholder="Say hello!" />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button type="submit" loading={loading} className="w-full">Send Request</Button>
      </form>
    </Modal>
  );
}
