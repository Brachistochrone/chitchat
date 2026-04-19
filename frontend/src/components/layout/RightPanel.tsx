import { Info } from 'lucide-react';

export default function RightPanel() {
  return (
    <aside className="flex w-72 flex-col border-l border-gray-200 bg-gray-50">
      <div className="flex flex-1 flex-col items-center justify-center text-gray-400">
        <Info className="h-10 w-10 mb-3 text-gray-300" />
        <p className="text-sm">Room info and members will appear here</p>
      </div>
    </aside>
  );
}
