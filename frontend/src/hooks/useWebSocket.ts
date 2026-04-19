import { useEffect, useRef } from 'react';
import { useAuthStore } from '../stores/useAuthStore';
import { useUnreadStore } from '../stores/useUnreadStore';
import { wsService } from '../api/websocket';

export function useWebSocket() {
  const token = useAuthStore((s) => s.token);
  const increment = useUnreadStore((s) => s.increment);
  const heartbeatRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (!token) return;

    wsService.connect(token, () => {
      wsService.subscribe('/user/queue/notifications', (event: any) => {
        if (event.type === 'UNREAD_UPDATE' && event.targetUserId) {
          increment(`room:${event.targetUserId}`);
        }
      });

      heartbeatRef.current = setInterval(() => {
        wsService.send('/app/presence/heartbeat', {});
      }, 30_000);
    });

    return () => {
      if (heartbeatRef.current) clearInterval(heartbeatRef.current);
      wsService.disconnect();
    };
  }, [token, increment]);

  return wsService;
}
