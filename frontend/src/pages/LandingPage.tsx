import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MessageCircle } from 'lucide-react';
import Button from '../components/ui/Button';
import LoginModal from '../components/auth/LoginModal';
import RegisterModal from '../components/auth/RegisterModal';
import ForgotPasswordModal from '../components/auth/ForgotPasswordModal';
import { useAuthStore } from '../stores/useAuthStore';

type ModalType = 'login' | 'register' | 'forgot' | null;

export default function LandingPage() {
  const [activeModal, setActiveModal] = useState<ModalType>(null);
  const { isAuthenticated } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) navigate('/chat');
  }, [isAuthenticated, navigate]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-indigo-50 to-white">
      <div className="text-center">
        <div className="mb-6 flex items-center justify-center gap-3">
          <MessageCircle className="h-16 w-16 text-indigo-600" />
          <h1 className="text-5xl font-bold text-gray-900">Chitchat</h1>
        </div>
        <p className="mb-8 text-lg text-gray-500">Connect, chat, and collaborate in real time</p>
        <div className="flex gap-4 justify-center">
          <Button size="lg" onClick={() => setActiveModal('login')}>
            Sign In
          </Button>
          <Button size="lg" variant="secondary" onClick={() => setActiveModal('register')}>
            Register
          </Button>
        </div>
      </div>

      <LoginModal
        isOpen={activeModal === 'login'}
        onClose={() => setActiveModal(null)}
        onSwitchToForgot={() => setActiveModal('forgot')}
      />
      <RegisterModal
        isOpen={activeModal === 'register'}
        onClose={() => setActiveModal(null)}
      />
      <ForgotPasswordModal
        isOpen={activeModal === 'forgot'}
        onClose={() => setActiveModal(null)}
      />
    </div>
  );
}
