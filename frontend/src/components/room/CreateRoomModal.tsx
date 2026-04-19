import { useState } from 'react';
import toast from 'react-hot-toast';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import { useRoomStore } from '../../stores/useRoomStore';

interface CreateRoomModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function CreateRoomModal({ isOpen, onClose }: CreateRoomModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [visibility, setVisibility] = useState<'PUBLIC' | 'PRIVATE'>('PUBLIC');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const createRoom = useRoomStore((s) => s.createRoom);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await createRoom(name, description || null, visibility);
      toast.success('Room created!');
      setName('');
      setDescription('');
      setVisibility('PUBLIC');
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create room');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create Room">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Room Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g. General"
          required
          maxLength={100}
        />
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="What's this room about?"
            maxLength={2000}
            rows={3}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm
              placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none
              focus:ring-1 focus:ring-indigo-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Visibility</label>
          <div className="flex gap-4">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                checked={visibility === 'PUBLIC'}
                onChange={() => setVisibility('PUBLIC')}
                className="text-indigo-600"
              />
              Public
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                checked={visibility === 'PRIVATE'}
                onChange={() => setVisibility('PRIVATE')}
                className="text-indigo-600"
              />
              Private
            </label>
          </div>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button type="submit" loading={loading} className="w-full">
          Create Room
        </Button>
      </form>
    </Modal>
  );
}
