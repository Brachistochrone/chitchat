import client from './client';
import type { Contact } from '../types/api';

export const contactsApi = {
  getFriends: () =>
    client.get<Contact[]>('/contacts'),

  getIncomingRequests: () =>
    client.get<Contact[]>('/contacts/requests/incoming'),

  sendFriendRequest: (targetUsername: string, message?: string) =>
    client.post<Contact>('/contacts/requests', { targetUsername, message }),

  acceptFriendRequest: (requestId: number) =>
    client.put<Contact>(`/contacts/requests/${requestId}/accept`),

  declineFriendRequest: (requestId: number) =>
    client.delete(`/contacts/requests/${requestId}`),

  removeFriend: (userId: number) =>
    client.delete(`/contacts/${userId}`),

  banUser: (userId: number) =>
    client.post(`/contacts/${userId}/ban`),

  unbanUser: (userId: number) =>
    client.delete(`/contacts/${userId}/ban`),
};
