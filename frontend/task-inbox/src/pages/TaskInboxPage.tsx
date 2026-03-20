import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Badge,
  PageLayout,
  PageHeader,
  Spinner,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Button,
} from "@workflow/ui-common";
import type { BadgeProps } from "@workflow/ui-common";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";
import { useMyTasks, useUnassignedTasks, useCompletedTasks } from "@/hooks/useTasks";
import type { TaskPriority, TaskStatus, TaskDto } from "@/api/taskApi";

type TabId = "my" | "unassigned" | "completed";

const PRIORITY_BADGE: Record<TaskPriority, BadgeProps["variant"]> = {
  LOW: "secondary",
  MEDIUM: "info",
  HIGH: "warning",
  CRITICAL: "destructive",
};

const STATUS_BADGE: Record<TaskStatus, BadgeProps["variant"]> = {
  CREATED: "secondary",
  ASSIGNED: "info",
  IN_PROGRESS: "warning",
  COMPLETED: "success",
  CANCELLED: "outline",
};

function formatDueDate(dueDate?: string): { text: string; urgent: boolean } {
  if (!dueDate) return { text: "—", urgent: false };
  const due = new Date(dueDate);
  const now = new Date();
  const diffMs = due.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  const urgent = diffDays <= 1;
  if (diffDays < 0) return { text: `${Math.abs(diffDays)}d overdue`, urgent: true };
  if (diffDays === 0) return { text: "Due today", urgent: true };
  if (diffDays === 1) return { text: "Due tomorrow", urgent: true };
  return { text: due.toLocaleDateString(), urgent: false };
}

function TaskTable({
  tasks,
  isLoading,
  onRowClick,
}: {
  tasks: TaskDto[];
  isLoading: boolean;
  onRowClick: (id: string) => void;
}) {
  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" className="text-primary" />
      </div>
    );
  }

  return (
    <div className="rounded-lg border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Task Name</TableHead>
            <TableHead>Process</TableHead>
            <TableHead>Created</TableHead>
            <TableHead>Due</TableHead>
            <TableHead>Priority</TableHead>
            <TableHead>Status</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tasks.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="h-24 text-center text-muted-foreground">
                No tasks found.
              </TableCell>
            </TableRow>
          )}
          {tasks.map((task) => {
            const due = formatDueDate(task.dueDate);
            return (
              <TableRow
                key={task.id}
                className="cursor-pointer"
                onClick={() => onRowClick(task.id)}
              >
                <TableCell className="font-medium">{task.name}</TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {task.processDefinitionName || task.processDefinitionKey}
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {new Date(task.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell className={due.urgent ? "font-medium text-destructive" : "text-sm text-muted-foreground"}>
                  {due.text}
                </TableCell>
                <TableCell>
                  <Badge variant={PRIORITY_BADGE[task.priority]}>{task.priority}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS_BADGE[task.status]}>{task.status}</Badge>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

export default function TaskInboxPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get("tab") as TabId | null;
  const [activeTab, setActiveTab] = useState<TabId>(tabParam ?? "my");

  const myTasks = useMyTasks();
  const unassignedTasks = useUnassignedTasks();
  const completedTasks = useCompletedTasks();

  const handleTabChange = (tab: TabId) => {
    setActiveTab(tab);
    setSearchParams(tab !== "my" ? { tab } : {});
  };

  const tabs: { id: TabId; label: string; count?: number }[] = [
    { id: "my", label: "My Tasks", count: myTasks.data?.totalElements },
    { id: "unassigned", label: "Unassigned", count: unassignedTasks.data?.totalElements },
    { id: "completed", label: "Completed", count: completedTasks.data?.totalElements },
  ];

  const currentData =
    activeTab === "my"
      ? myTasks
      : activeTab === "unassigned"
      ? unassignedTasks
      : completedTasks;

  return (
    <PageLayout sidebar={<Sidebar />} header={<Header />}>
      <PageHeader
        title="Task Inbox"
        description="Manage and complete your workflow tasks"
      />

      {/* Tab navigation */}
      <div className="mb-4 flex gap-1 rounded-lg border bg-muted p-1 w-fit">
        {tabs.map((tab) => (
          <Button
            key={tab.id}
            variant={activeTab === tab.id ? "default" : "ghost"}
            size="sm"
            onClick={() => handleTabChange(tab.id)}
            className={activeTab === tab.id ? "" : "text-muted-foreground"}
          >
            {tab.label}
            {tab.count !== undefined && tab.count > 0 && (
              <Badge
                variant={activeTab === tab.id ? "secondary" : "outline"}
                className="ml-1.5 h-5 min-w-5 justify-center px-1 text-xs"
              >
                {tab.count}
              </Badge>
            )}
          </Button>
        ))}
      </div>

      <TaskTable
        tasks={currentData.data?.content ?? []}
        isLoading={currentData.isLoading}
        onRowClick={(id) => navigate(`/tasks/${id}`)}
      />
    </PageLayout>
  );
}
