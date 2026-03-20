import { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { PlusCircle, Upload, Pencil, Play } from "lucide-react";
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
  DialogFooter,
  Input,
  Label,
} from "@workflow/ui-common";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";
import {
  listProcessDefinitions,
  deployProcess,
  startProcessInstance,
  type ProcessDefinitionDto,
} from "@/api/workflowApi";

export default function ProcessListPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [deployDialogOpen, setDeployDialogOpen] = useState(false);
  const [deployName, setDeployName] = useState("");
  const [deployFile, setDeployFile] = useState<File | null>(null);
  const [deployError, setDeployError] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["process-definitions"],
    queryFn: () => listProcessDefinitions({ size: 50 }),
  });

  const deployMutation = useMutation({
    mutationFn: async () => {
      if (!deployFile || !deployName.trim()) {
        throw new Error("Name and BPMN file are required");
      }
      const bpmnXml = await deployFile.text();
      return deployProcess({ name: deployName.trim(), bpmnXml });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["process-definitions"] });
      queryClient.invalidateQueries({ queryKey: ["deployments"] });
      setDeployDialogOpen(false);
      setDeployName("");
      setDeployFile(null);
      setDeployError(null);
    },
    onError: (err: Error) => {
      setDeployError(err.message);
    },
  });

  const startMutation = useMutation({
    mutationFn: (def: ProcessDefinitionDto) =>
      startProcessInstance({ processDefinitionKey: def.key }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["process-instances"] });
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setDeployFile(file);
    if (file && !deployName) {
      setDeployName(file.name.replace(/\.bpmn$/i, ""));
    }
  };

  return (
    <PageLayout
      sidebar={<Sidebar />}
      header={<Header />}
    >
      <PageHeader
        title="Process Definitions"
        description="Manage and deploy BPMN process definitions"
        actions={
          <>
            <Button onClick={() => navigate("/processes/new/designer")} variant="outline" size="sm">
              <PlusCircle className="h-4 w-4" />
              New Process
            </Button>
            <Button onClick={() => setDeployDialogOpen(true)} size="sm">
              <Upload className="h-4 w-4" />
              Deploy BPMN
            </Button>
          </>
        }
      />

      {isLoading && (
        <div className="flex justify-center py-12">
          <Spinner size="lg" className="text-primary" />
        </div>
      )}

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4">
          <p className="text-sm text-destructive">Failed to load process definitions.</p>
        </div>
      )}

      {data && (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Key</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Deployed At</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                    No process definitions found. Deploy a BPMN file to get started.
                  </TableCell>
                </TableRow>
              )}
              {data.content.map((def) => (
                <TableRow key={def.id}>
                  <TableCell className="font-medium">{def.name || def.key}</TableCell>
                  <TableCell>
                    <code className="rounded bg-muted px-1.5 py-0.5 text-xs">{def.key}</code>
                  </TableCell>
                  <TableCell>
                    <Badge variant="secondary">v{def.version}</Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground text-sm">
                    {new Date(def.deployedAt).toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => navigate(`/processes/${def.id}/designer`)}
                        title="Open in designer"
                      >
                        <Pencil className="h-4 w-4" />
                        Design
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => startMutation.mutate(def)}
                        disabled={startMutation.isPending}
                        title="Start a new instance"
                      >
                        <Play className="h-4 w-4" />
                        Start
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Deploy Dialog */}
      <Dialog open={deployDialogOpen} onOpenChange={setDeployDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Deploy BPMN Process</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="deploy-name">Deployment Name</Label>
              <Input
                id="deploy-name"
                placeholder="e.g. Order Processing v2"
                value={deployName}
                onChange={(e) => setDeployName(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="deploy-file">BPMN File</Label>
              <input
                id="deploy-file"
                type="file"
                accept=".bpmn,.xml"
                ref={fileInputRef}
                onChange={handleFileChange}
                className="block w-full text-sm text-muted-foreground file:mr-4 file:rounded-md file:border-0 file:bg-primary file:px-4 file:py-2 file:text-xs file:font-medium file:text-primary-foreground hover:file:bg-primary/90"
              />
              {deployFile && (
                <p className="text-xs text-muted-foreground">
                  Selected: {deployFile.name} ({(deployFile.size / 1024).toFixed(1)} KB)
                </p>
              )}
            </div>

            {deployError && (
              <p className="text-sm text-destructive">{deployError}</p>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDeployDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={() => deployMutation.mutate()}
              disabled={deployMutation.isPending || !deployFile || !deployName.trim()}
            >
              {deployMutation.isPending ? (
                <>
                  <Spinner size="sm" />
                  Deploying...
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4" />
                  Deploy
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
