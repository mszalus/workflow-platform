import { LogOut, User, ChevronDown } from "lucide-react";
import {
  Button,
  Badge,
} from "@workflow/ui-common";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useAuth } from "@/hooks/useAuth";

export default function Header() {
  const { user, signOut } = useAuth();

  return (
    <div className="flex w-full items-center justify-between">
      <div />

      <div className="flex items-center gap-3">
        {user?.tenantId && (
          <Badge variant="outline" className="text-xs">
            {user.tenantId}
          </Badge>
        )}

        <DropdownMenu.Root>
          <DropdownMenu.Trigger asChild>
            <Button variant="ghost" size="sm" className="gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10">
                <User className="h-4 w-4 text-primary" />
              </div>
              <span className="text-sm font-medium">{user?.name ?? "User"}</span>
              <ChevronDown className="h-3 w-3 text-muted-foreground" />
            </Button>
          </DropdownMenu.Trigger>

          <DropdownMenu.Portal>
            <DropdownMenu.Content
              className="z-50 min-w-[180px] overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-md animate-in fade-in-0 zoom-in-95"
              align="end"
              sideOffset={4}
            >
              <div className="px-2 py-1.5">
                <p className="text-sm font-medium">{user?.name}</p>
                <p className="text-xs text-muted-foreground">{user?.email}</p>
              </div>
              <DropdownMenu.Separator className="my-1 h-px bg-muted" />
              <DropdownMenu.Item
                className="flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent"
                onSelect={() => signOut()}
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </DropdownMenu.Item>
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>
      </div>
    </div>
  );
}
