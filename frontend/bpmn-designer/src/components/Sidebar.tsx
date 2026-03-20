import { NavLink } from "react-router-dom";
import { LayoutDashboard, FileCode2, Rocket, Database, Activity } from "lucide-react";
import { cn } from "@workflow/ui-common";

interface NavItem {
  label: string;
  to: string;
  icon: React.ElementType;
}

const navItems: NavItem[] = [
  { label: "Processes", to: "/processes", icon: FileCode2 },
  { label: "Deployments", to: "/deployments", icon: Rocket },
  { label: "Schemas", to: "/schemas", icon: Database },
  { label: "Monitoring", to: "/monitoring", icon: Activity },
];

export default function Sidebar() {
  return (
    <div className="flex h-full flex-col">
      {/* Logo */}
      <div className="flex h-14 items-center gap-2 border-b px-4">
        <LayoutDashboard className="h-5 w-5 text-primary" />
        <span className="font-semibold text-sm">BPMN Designer</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto p-2">
        <ul className="space-y-1">
          {navItems.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
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
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {/* Version footer */}
      <div className="border-t p-4">
        <p className="text-xs text-muted-foreground">Workflow Designer v1.0</p>
      </div>
    </div>
  );
}
