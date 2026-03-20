import * as React from "react";
import { cn } from "../lib/utils";

export interface PageLayoutProps {
  sidebar?: React.ReactNode;
  header?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

export function PageLayout({ sidebar, header, children, className }: PageLayoutProps) {
  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      {sidebar && (
        <aside className="flex h-full w-64 flex-shrink-0 flex-col border-r bg-card">
          {sidebar}
        </aside>
      )}
      <div className="flex flex-1 flex-col overflow-hidden">
        {header && (
          <header className="flex h-14 flex-shrink-0 items-center border-b bg-card px-4">
            {header}
          </header>
        )}
        <main className={cn("flex-1 overflow-auto p-6", className)}>
          {children}
        </main>
      </div>
    </div>
  );
}

export interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  className?: string;
}

export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
  return (
    <div className={cn("flex items-start justify-between mb-6", className)}>
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {description && (
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}
