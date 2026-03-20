import { useNavigate } from "react-router-dom";
import { Bell, BellOff, CheckCheck, ExternalLink, Wifi, WifiOff } from "lucide-react";
import {
  Button,
  Badge,
  PageLayout,
  PageHeader,
  Spinner,
  cn,
} from "@workflow/ui-common";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";
import { useNotifications } from "@/hooks/useNotifications";
import type { NotificationDto, NotificationCategory } from "@/api/notificationApi";

const CATEGORY_LABELS: Record<NotificationCategory, string> = {
  TASK_ASSIGNED: "Task Assigned",
  TASK_DUE_SOON: "Due Soon",
  TASK_OVERDUE: "Overdue",
  PROCESS_COMPLETED: "Process Completed",
  MENTION: "Mention",
  SYSTEM: "System",
};

function NotificationItem({
  notification,
  onMarkRead,
  onNavigate,
}: {
  notification: NotificationDto;
  onMarkRead: (id: string) => void;
  onNavigate: (notification: NotificationDto) => void;
}) {
  const hasLink = notification.taskId || notification.processInstanceId;

  return (
    <div
      className={cn(
        "flex gap-4 rounded-lg border p-4 transition-colors",
        !notification.read
          ? "border-primary/30 bg-primary/5"
          : "bg-card hover:bg-accent/50"
      )}
    >
      {/* Unread indicator */}
      <div className="flex flex-shrink-0 items-start pt-1">
        {!notification.read ? (
          <div className="h-2 w-2 rounded-full bg-primary" />
        ) : (
          <div className="h-2 w-2 rounded-full bg-muted" />
        )}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <div>
            <div className="flex items-center gap-2">
              <p className={cn("text-sm", !notification.read && "font-semibold")}>
                {notification.title}
              </p>
              <Badge variant="outline" className="text-xs">
                {CATEGORY_LABELS[notification.category] ?? notification.category}
              </Badge>
            </div>
            <p className="mt-0.5 text-sm text-muted-foreground">{notification.message}</p>
            <p className="mt-1 text-xs text-muted-foreground">
              {new Date(notification.createdAt).toLocaleString()}
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-1">
            {hasLink && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onNavigate(notification)}
                title="Go to related item"
              >
                <ExternalLink className="h-4 w-4" />
              </Button>
            )}
            {!notification.read && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onMarkRead(notification.id)}
                title="Mark as read"
              >
                <CheckCheck className="h-4 w-4" />
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const {
    notifications,
    unreadCount,
    isConnected,
    isLoading,
    markAsRead,
    markAllRead,
    refresh,
  } = useNotifications();

  const handleNavigate = (notification: NotificationDto) => {
    if (notification.taskId) {
      navigate(`/tasks/${notification.taskId}`);
    } else if (notification.processInstanceId) {
      // Opens in monitoring (would be in another app in production)
      window.open(`/monitoring?instanceId=${notification.processInstanceId}`, "_blank");
    }
  };

  return (
    <PageLayout sidebar={<Sidebar />} header={<Header />}>
      <PageHeader
        title="Notifications"
        description="Stay up to date with your workflow activities"
        actions={
          <div className="flex items-center gap-2">
            {/* WebSocket connection indicator */}
            <div
              className="flex items-center gap-1.5 text-xs text-muted-foreground"
              title={isConnected ? "Real-time notifications active" : "Disconnected from notification service"}
            >
              {isConnected ? (
                <Wifi className="h-3.5 w-3.5 text-green-500" />
              ) : (
                <WifiOff className="h-3.5 w-3.5 text-muted-foreground" />
              )}
              {isConnected ? "Live" : "Offline"}
            </div>

            <Button variant="outline" size="sm" onClick={refresh} disabled={isLoading}>
              Refresh
            </Button>

            {unreadCount > 0 && (
              <Button size="sm" onClick={markAllRead}>
                <CheckCheck className="h-4 w-4" />
                Mark All Read
                <Badge variant="secondary" className="ml-1">
                  {unreadCount}
                </Badge>
              </Button>
            )}
          </div>
        }
      />

      {isLoading && (
        <div className="flex justify-center py-12">
          <Spinner size="lg" className="text-primary" />
        </div>
      )}

      {!isLoading && notifications.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
            <BellOff className="h-8 w-8 text-muted-foreground" />
          </div>
          <h3 className="text-lg font-semibold">No notifications</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            You&apos;re all caught up! New notifications will appear here.
          </p>
        </div>
      )}

      {!isLoading && notifications.length > 0 && (
        <div className="space-y-2">
          {/* Unread section */}
          {unreadCount > 0 && (
            <div className="mb-4">
              <div className="mb-2 flex items-center gap-2">
                <Bell className="h-4 w-4 text-primary" />
                <span className="text-sm font-medium">
                  Unread ({unreadCount})
                </span>
              </div>
              <div className="space-y-2">
                {notifications
                  .filter((n) => !n.read)
                  .map((notification) => (
                    <NotificationItem
                      key={notification.id}
                      notification={notification}
                      onMarkRead={markAsRead}
                      onNavigate={handleNavigate}
                    />
                  ))}
              </div>
            </div>
          )}

          {/* Read section */}
          {notifications.some((n) => n.read) && (
            <div>
              <div className="mb-2 flex items-center gap-2">
                <span className="text-sm font-medium text-muted-foreground">
                  Earlier
                </span>
              </div>
              <div className="space-y-2">
                {notifications
                  .filter((n) => n.read)
                  .map((notification) => (
                    <NotificationItem
                      key={notification.id}
                      notification={notification}
                      onMarkRead={markAsRead}
                      onNavigate={handleNavigate}
                    />
                  ))}
              </div>
            </div>
          )}
        </div>
      )}
    </PageLayout>
  );
}
