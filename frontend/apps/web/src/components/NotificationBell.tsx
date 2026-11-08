import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient, type Notification, type PagedResponse } from '@wfp/shared-ui';

const POLL_INTERVAL_MS = 30_000;

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () =>
      apiClient.get<{ count: number }>('/notifications/unread-count').then((response) => response.data.count),
    refetchInterval: POLL_INTERVAL_MS,
  });
  const { data: notifications = [] } = useQuery({
    queryKey: ['notifications', 'latest'],
    queryFn: () =>
      apiClient.get<PagedResponse<Notification>>('/notifications').then((response) => response.data.content),
    enabled: open,
  });

  const refreshNotifications = () => queryClient.invalidateQueries({ queryKey: ['notifications'] });
  const markRead = useMutation({
    mutationFn: (id: string) => apiClient.put('/notifications/mark-read', [id]),
    onSuccess: refreshNotifications,
  });
  const markAllRead = useMutation({
    mutationFn: () => apiClient.put('/notifications/mark-all-read'),
    onSuccess: refreshNotifications,
  });

  const openNotification = (notification: Notification) => {
    markRead.mutate(notification.id);
    setOpen(false);
    if (notification.referenceType === 'ITEM') {
      navigate(`/items/${notification.referenceId}`);
    }
  };

  return (
    <div style={{ position: 'relative' }}>
      <button aria-label={`Notifications, ${unreadCount} unread`} onClick={() => setOpen(!open)}>
        Bell {unreadCount > 0 && <strong>{unreadCount}</strong>}
      </button>
      {open && (
        <div
          style={{
            position: 'absolute',
            right: 0,
            width: 320,
            maxHeight: 400,
            overflow: 'auto',
            background: '#fff',
            border: '1px solid #ccc',
            borderRadius: 4,
            zIndex: 10,
          }}
        >
          <button onClick={() => markAllRead.mutate()} style={{ margin: '0.5rem' }}>
            Mark all read
          </button>
          {notifications.length === 0 && <div style={{ padding: '0.5rem' }}>No notifications</div>}
          {notifications.map((notification) => (
            <button
              key={notification.id}
              onClick={() => openNotification(notification)}
              style={{
                display: 'block',
                width: '100%',
                padding: '0.5rem',
                textAlign: 'left',
                border: 'none',
                borderTop: '1px solid #eee',
                background: notification.read ? '#fff' : '#e3f2fd',
                cursor: 'pointer',
              }}
            >
              <div style={{ fontWeight: notification.read ? 'normal' : 'bold' }}>{notification.title}</div>
              <div style={{ fontSize: '0.85rem', color: '#666' }}>{notification.message}</div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
