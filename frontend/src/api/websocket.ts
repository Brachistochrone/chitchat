import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client: Client | null = null;

export const wsService = {
  connect(token: string, onConnect?: () => void) {
    if (client?.connected) return;

    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('STOMP connected');
        onConnect?.();
      },
      onDisconnect: () => console.log('STOMP disconnected'),
      onStompError: (frame) => console.error('STOMP error:', frame.headers['message']),
    });

    client.activate();
  },

  disconnect() {
    if (client) {
      client.deactivate();
      client = null;
    }
  },

  subscribe(destination: string, callback: (body: any) => void): StompSubscription | null {
    if (!client?.connected) return null;
    return client.subscribe(destination, (msg: IMessage) => {
      try {
        callback(JSON.parse(msg.body));
      } catch {
        callback(msg.body);
      }
    });
  },

  send(destination: string, body: any) {
    if (!client?.connected) return;
    client.publish({ destination, body: JSON.stringify(body) });
  },

  get connected() {
    return client?.connected ?? false;
  },
};
