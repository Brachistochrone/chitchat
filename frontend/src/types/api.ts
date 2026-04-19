export interface User {
  id: number;
  username: string;
  displayName: string | null;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Room {
  id: number;
  name: string;
  description: string | null;
  visibility: 'PUBLIC' | 'PRIVATE';
  owner: User;
  memberCount: number;
  createdAt: string;
}

export interface Message {
  id: number;
  chatType: 'ROOM' | 'PERSONAL';
  sender: User;
  content: string | null;
  replyTo: Message | null;
  attachments: Attachment[];
  editedAt: string | null;
  createdAt: string;
}

export interface Attachment {
  id: number;
  originalFilename: string;
  fileSize: number;
  mimeType: string | null;
  comment: string | null;
  downloadUrl: string;
}

export interface Session {
  id: number;
  browser: string | null;
  ipAddress: string | null;
  lastSeenAt: string;
  current: boolean;
}

export interface Contact {
  id: number;
  user: User;
  status: 'PENDING' | 'ACCEPTED';
  message: string | null;
  createdAt: string;
}

export interface MemberResponse {
  user: User;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: string;
}

export interface UnreadCount {
  roomId: number | null;
  chatUserId: number | null;
  count: number;
}
