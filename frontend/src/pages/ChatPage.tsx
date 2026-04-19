import TopNav from '../components/layout/TopNav';
import Sidebar from '../components/layout/Sidebar';
import ChatArea from '../components/layout/ChatArea';
import RightPanel from '../components/layout/RightPanel';

export default function ChatPage() {
  return (
    <div className="flex h-screen flex-col">
      <TopNav />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <ChatArea />
        <RightPanel />
      </div>
    </div>
  );
}
