import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Trash2, ChevronDown, ChevronRight } from "lucide-react";
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
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@workflow/ui-common";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";
import { listDeployments, deleteDeployment, type DeploymentDto } from "@/api/workflowApi";

export default function DeploymentListPage() {
  const queryClient = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<DeploymentDto | null>(null);
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const { data, isLoading, error } = useQuery({
    queryKey: ["deployments"],
    queryFn: () => listDeployments({ size: 50 }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteDeployment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["deployments"] });
      queryClient.invalidateQueries({ queryKey: ["process-definitions"] });
      setDeleteTarget(null);
    },
  });

  const toggleExpand = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  return (
    <PageLayout sidebar={<Sidebar />} header={<Header />}>
      <PageHeader
        title="Deployments"
        description="View and manage BPMN deployments"
      />

      {isLoading && (
        <div className="flex justify-center py-12">
          <Spinner size="lg" className="text-primary" />
        </div>
      )}

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4">
          <p className="text-sm text-destructive">Failed to load deployments.</p>
        </div>
      )}

      {data && (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-8" />
                <TableHead>Name</TableHead>
                <TableHead>Processes</TableHead>
                <TableHead>Deployed At</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                    No deployments found.
                  </TableCell>
                </TableRow>
              )}
              {data.content.map((deployment) => (
                <>
                  <TableRow
                    key={deployment.id}
                    className="cursor-pointer"
                    onClick={() => toggleExpand(deployment.id)}
                  >
                    <TableCell>
                      {expandedIds.has(deployment.id) ? (
                        <ChevronDown className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <ChevronRight className="h-4 w-4 text-muted-foreground" />
                      )}
                    </TableCell>
                    <TableCell className="font-medium">{deployment.name}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">
                        {deployment.processDefinitions.length} process
                        {deployment.processDefinitions.length !== 1 ? "es" : ""}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(deployment.deployedAt).toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive hover:text-destructive"
                        onClick={() => setDeleteTarget(deployment)}
                      >
                        <Trash2 className="h-4 w-4" />
                        Delete
                      </Button>
                    </TableCell>
                  </TableRow>

                  {expandedIds.has(deployment.id) &&
                    deployment.processDefinitions.map((proc) => (
                      <TableRow key={proc.id} className="bg-muted/30">
                        <TableCell />
                        <TableCell className="pl-6 text-sm text-muted-foreground">
                          {proc.name || proc.key}
                        </TableCell>
                        <TableCell>
                          <code className="rounded bg-muted px-1.5 py-0.5 text-xs">
                            {proc.key}
                          </code>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">v{proc.version}</Badge>
                        </TableCell>
                        <TableCell />
                      </TableRow>
                    ))}
                </>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Deployment</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete the deployment{" "}
              <strong>&ldquo;{deleteTarget?.name}&rdquo;</strong>? This will also remove all
              associated process definitions. Running instances will not be affected.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? (
                <>
                  <Spinner size="sm" />
                  Deleting...
                </>
              ) : (
                <>
                  <Trash2 className="h-4 w-4" />
                  Delete
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
