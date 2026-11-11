import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ActionIcon, Button, Group, Indicator, Paper, Text, UnstyledButton } from '@mantine/core';
import { apiClient, type Notification, type PagedResponse } from '../shared-ui';
import { BORDER, SUBTLE_INK } from '../theme';

const POLL_INTERVAL_MS = 30_000;

function BellIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" strokeLinecap="round" />
    </svg>
  );
}

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
      <Indicator label={unreadCount} size={18} color="red" offset={4} disabled={unreadCount === 0}>
        <ActionIcon
          variant="subtle"
          color="gray"
          size="lg"
          radius="xl"
          aria-label={`Notifications, ${unreadCount} unread`}
          onClick={() => setOpen(!open)}
        >
          <BellIcon />
        </ActionIcon>
      </Indicator>
      {open && (
        <Paper
          shadow="md"
          withBorder
          style={{ position: 'absolute', right: 0, top: 44, width: 360, maxHeight: 420, overflow: 'auto', zIndex: 10 }}
        >
          <Group justify="space-between" p="sm">
            <Text fw={600}>Notifications</Text>
            <Button variant="subtle" size="compact-sm" onClick={() => markAllRead.mutate()}>
              Mark all read
            </Button>
          </Group>
          {notifications.length === 0 && (
            <Text p="sm" c={SUBTLE_INK}>
              No notifications
            </Text>
          )}
          {notifications.map((notification) => (
            <UnstyledButton
              key={notification.id}
              onClick={() => openNotification(notification)}
              style={{
                display: 'block',
                width: '100%',
                padding: '0.75rem',
                borderTop: `1px solid ${BORDER}`,
                background: notification.read ? undefined : '#deebff',
              }}
            >
              <Text size="sm" fw={notification.read ? 400 : 600}>
                {notification.title}
              </Text>
              <Text size="sm" c={SUBTLE_INK}>
                {notification.message}
              </Text>
            </UnstyledButton>
          ))}
        </Paper>
      )}
    </div>
  );
}
