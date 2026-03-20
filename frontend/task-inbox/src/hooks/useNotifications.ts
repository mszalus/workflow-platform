import { useState, useEffect, useCallback, useRef } from "react";
import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useAuth } from "./useAuth";
import {
  getNotifications,
  markAsRead as apiMarkAsRead,
  markAllRead as apiMarkAllRead,
  type NotificationDto,
} from "@/api/notificationApi";

const WS_URL =
  (import.meta.env.VITE_NOTIFICATION_SERVICE_WS_URL as string | undefined) ??
  "http://localhost:8083/ws";

export interface UseNotificationsReturn {
  notifications: NotificationDto[];
  unreadCount: number;
  isConnected: boolean;
  isLoading: boolean;
  markAsRead: (id: string) => Promise<void>;
  markAllRead: () => Promise<void>;
  refresh: () => void;
}

export function useNotifications(): UseNotificationsReturn {
  const { user, isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const stompClientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<StompSubscription | null>(null);

  const fetchNotifications = useCallback(async () => {
    if (!isAuthenticated) return;
    setIsLoading(true);
    try {
      const result = await getNotifications({ size: 50 });
      setNotifications(result.content);
    } catch (err) {
      console.error("Failed to fetch notifications:", err);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  // Initial load
  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  // WebSocket / STOMP connection
  useEffect(() => {
    if (!isAuthenticated || !user?.accessToken) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${user.accessToken}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setIsConnected(true);

        // Subscribe to personal notification queue
        subscriptionRef.current = client.subscribe(
          "/user/queue/notifications",
          (message) => {
            try {
              const notification = JSON.parse(message.body) as NotificationDto;
              setNotifications((prev) => [notification, ...prev]);
            } catch (err) {
              console.error("Failed to parse notification message:", err);
            }
          }
        );
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onStompError: (frame) => {
        console.error("STOMP error:", frame);
        setIsConnected(false);
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      subscriptionRef.current?.unsubscribe();
      client.deactivate();
      stompClientRef.current = null;
    };
  }, [isAuthenticated, user?.accessToken]);

  const markAsRead = useCallback(async (id: string) => {
    try {
      const updated = await apiMarkAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? updated : n))
      );
    } catch (err) {
      console.error("Failed to mark notification as read:", err);
    }
  }, []);

  const markAllRead = useCallback(async () => {
    try {
      await apiMarkAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch (err) {
      console.error("Failed to mark all notifications as read:", err);
    }
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return {
    notifications,
    unreadCount,
    isConnected,
    isLoading,
    markAsRead,
    markAllRead,
    refresh: fetchNotifications,
  };
}
