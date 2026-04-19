import { Hash, Users, Plus } from 'lucide-react';

export default function Sidebar() {
  return (
    <aside className="flex w-64 flex-col border-r border-gray-200 bg-gray-50">
      <div className="p-3">
        <input
          type="text"
          placeholder="Search..."
          className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm
            placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>

      <div className="flex-1 overflow-y-auto px-2">
        <div className="mb-4">
          <h3 className="flex items-center gap-1.5 px-2 py-1.5 text-xs font-semibold uppercase text-gray-500">
            <Hash className="h-3.5 w-3.5" />
            Rooms
          </h3>
          <p className="px-3 py-2 text-sm text-gray-400">Rooms will appear here</p>
        </div>

        <div className="mb-4">
          <h3 className="flex items-center gap-1.5 px-2 py-1.5 text-xs font-semibold uppercase text-gray-500">
            <Users className="h-3.5 w-3.5" />
            Contacts
          </h3>
          <p className="px-3 py-2 text-sm text-gray-400">Contacts will appear here</p>
        </div>
      </div>

      <div className="border-t border-gray-200 p-3">
        <button className="flex w-full items-center justify-center gap-2 rounded-lg bg-indigo-600
          px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors">
          <Plus className="h-4 w-4" />
          Create Room
        </button>
      </div>
    </aside>
  );
}
