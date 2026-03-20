import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { RefreshCw, XCircle } from "lucide-react";
import {
  Button,
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@workflow/ui-common";
import type { BadgeProps } from "@workflow/ui-common";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";
import {
  listProcessDefinitions,
  listProcessInstances,
  cancelProcessInstance,
  type ProcessInstanceDto,
} from "@/api/workflowApi";

type InstanceState = ProcessInstanceDto["state"];

const STATE_BADGE: Record<InstanceState, BadgeProps["variant"]> = {
  ACTIVE: "info",
  SUSPENDED: "warning",
  COMPLETED: "success",
  CANCELLED: "secondary",
  INCIDENT: "destructive",
};

export default function ProcessMonitoringPage() {
  const queryClient = useQueryClient();
  const [filterKey, setFilterKey] = useState<string>("__all__");
  const [filterState, setFilterState] = useState<InstanceState | "__all__">("__all__");
  const [cancelTarget, setCancelTarget] = useState<ProcessInstanceDto | null>(null);

  const { data: processesData } = useQuery({
    queryKey: ["process-definitions"],
    queryFn: () => listProcessDefinitions({ size: 100 }),
  });

  const instancesQuery = useQuery({
    queryKey: ["process-instances", filterKey, filterState],
    queryFn: () =>
      listProcessInstances({
        processDefinitionKey: filterKey !== "__all__" ? filterKey : undefined,
        state: filterState !== "__all__" ? filterState : undefined,
        size: 100,
      }),
    refetchInterval: 10_000, // auto-refresh every 10 seconds
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => cancelProcessInstance(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["process-instances"] });
      setCancelTarget(null);
    },
  });

  return (
    <PageLayout sidebar={<Sidebar />} header={<Header />}>
      <PageHeader
        title="Process Monitoring"
        description="Monitor running and completed process instances"
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={() => instancesQuery.refetch()}
            disabled={instancesQuery.isFetching}
          >
            <RefreshCw
              className={`h-4 w-4 ${instancesQuery.isFetching ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        }
      />

      {/* Filters */}
      <div className="mb-4 flex items-center gap-3">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Process:</span>
          <Select value={filterKey} onValueChange={setFilterKey}>
            <SelectTrigger className="w-52">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__all__">All Processes</SelectItem>
              {processesData?.content.map((proc) => (
                <SelectItem key={proc.key} value={proc.key}>
                  {proc.name || proc.key}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">State:</span>
          <Select
            value={filterState}
            onValueChange={(v) => setFilterState(v as InstanceState | "__all__")}
          >
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__all__">All States</SelectItem>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="SUSPENDED">Suspended</SelectItem>
              <SelectItem value="COMPLETED">Completed</SelectItem>
              <SelectItem value="CANCELLED">Cancelled</SelectItem>
              <SelectItem value="INCIDENT">Incident</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {instancesQuery.data && (
          <span className="ml-auto text-sm text-muted-foreground">
            {instancesQuery.data.totalElements} instance
            {instancesQuery.data.totalElements !== 1 ? "s" : ""}
          </span>
        )}
      </div>

      {instancesQuery.isLoading && (
        <div className="flex justify-center py-12">
          <Spinner size="lg" className="text-primary" />
        </div>
      )}

      {instancesQuery.error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4">
          <p className="text-sm text-destructive">Failed to load process instances.</p>
        </div>
      )}

      {instancesQuery.data && (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Instance ID</TableHead>
                <TableHead>Process</TableHead>
                <TableHead>Business Key</TableHead>
                <TableHead>State</TableHead>
                <TableHead>Started By</TableHead>
                <TableHead>Started At</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {instancesQuery.data.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} className="h-24 text-center text-muted-foreground">
                    No process instances found.
                  </TableCell>
                </TableRow>
              )}
              {instancesQuery.data.content.map((instance) => (
                <TableRow key={instance.id}>
                  <TableCell>
                    <code className="rounded bg-muted px-1.5 py-0.5 text-xs">
                      {instance.id.slice(0, 8)}…
                    </code>
                  </TableCell>
                  <TableCell className="font-medium">
                    {instance.processDefinitionName || instance.processDefinitionKey}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {instance.businessKey ?? "—"}
                  </TableCell>
                  <TableCell>
                    <Badge variant={STATE_BADGE[instance.state]}>{instance.state}</Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {instance.startedBy}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {new Date(instance.startedAt).toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    {instance.state === "ACTIVE" && (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive hover:text-destructive"
                        onClick={() => setCancelTarget(instance)}
                      >
                        <XCircle className="h-4 w-4" />
                        Cancel
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Cancel Confirmation Dialog */}
      <Dialog open={!!cancelTarget} onOpenChange={(open) => !open && setCancelTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel Process Instance</DialogTitle>
            <DialogDescription>
              Are you sure you want to cancel instance{" "}
              <strong>{cancelTarget?.id.slice(0, 8)}</strong>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelTarget(null)}>
              Keep Running
            </Button>
            <Button
              variant="destructive"
              onClick={() => cancelTarget && cancelMutation.mutate(cancelTarget.id)}
              disabled={cancelMutation.isPending}
            >
              {cancelMutation.isPending ? <Spinner size="sm" /> : <XCircle className="h-4 w-4" />}
              Cancel Instance
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
