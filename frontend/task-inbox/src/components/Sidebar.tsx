import { NavLink } from "react-router-dom";
import { Inbox, Bell, CheckSquare, Workflow } from "lucide-react";
import { cn, Badge } from "@workflow/ui-common";
import { useNotifications } from "@/hooks/useNotifications";

interface NavItem {
  label: string;
  to: string;
  icon: React.ElementType;
  badge?: number;
}

export default function Sidebar() {
  const { unreadCount } = useNotifications();

  const navItems: NavItem[] = [
    { label: "Inbox", to: "/tasks", icon: Inbox },
    { label: "Notifications", to: "/notifications", icon: Bell, badge: unreadCount },
    { label: "Completed", to: "/tasks?tab=completed", icon: CheckSquare },
  ];

  return (
    <div className="flex h-full flex-col">
      {/* Logo */}
      <div className="flex h-14 items-center gap-2 border-b px-4">
        <Workflow className="h-5 w-5 text-primary" />
        <span className="font-semibold text-sm">Task Inbox</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto p-2">
        <ul className="space-y-1">
          {navItems.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.to === "/tasks"}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-primary text-primary-foreground"
                      : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                  )
                }
              >
                <item.icon className="h-4 w-4 shrink-0" />
                <span className="flex-1">{item.label}</span>
                {item.badge !== undefined && item.badge > 0 && (
                  <Badge variant="destructive" className="h-5 min-w-5 justify-center px-1 text-xs">
                    {item.badge > 99 ? "99+" : item.badge}
                  </Badge>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="border-t p-4">
        <p className="text-xs text-muted-foreground">Task Inbox v1.0</p>
      </div>
    </div>
  );
}
