import React, { useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useForm, type FieldValues } from "react-hook-form";
import {
  ArrowLeft,
  UserCheck,
  UserX,
  CheckCircle2,
  UserCog,
  AlertCircle,
  ExternalLink,
} from "lucide-react";
import {
  Button,
  Badge,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Spinner,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  Input,
  Label,
  PageLayout,
} from "@workflow/ui-common";
import type { BadgeProps, FieldDefinitionDto } from "@workflow/ui-common";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";
import { DynamicFieldRenderer } from "@/components/DynamicFieldRenderer";
import { useTask, useClaimTask, useUnclaimTask, useCompleteTask, useDelegateTask } from "@/hooks/useTasks";
import { useAuth } from "@/hooks/useAuth";
import { getFieldValues, saveFieldValues } from "@/api/fieldValueApi";
import { getSchemaByProcessKey } from "@/api/schemaApi";
import type { TaskPriority, TaskStatus } from "@/api/taskApi";
import apiClient from "@/api/apiClient";

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

export default function TaskDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const { data: task, isLoading: taskLoading, error: taskError } = useTask(id!);
  const claimTask = useClaimTask();
  const unclaimTask = useUnclaimTask();
  const completeTask = useCompleteTask();
  const delegateTask = useDelegateTask();

  const [delegateDialogOpen, setDelegateDialogOpen] = useState(false);
  const [delegateUserId, setDelegateUserId] = useState("");
  const [delegateComment, setDelegateComment] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Track enum select values separately (react-hook-form doesn't handle radix Select natively)
  const [selectValues, setSelectValues] = useState<Record<string, string>>({});
  // Track user search results
  const [userSearchResults, setUserSearchResults] = useState<
    Record<string, Array<{ id: string; name: string; email: string }>>
  >({});
  // File upload state: fieldKey -> attachment ID
  const [fileUploads, setFileUploads] = useState<Record<string, string>>({});

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
  } = useForm<FieldValues>();

  // Load schema for this process
  const { data: schemaData, isLoading: schemaLoading } = useQuery({
    queryKey: ["schema", task?.processDefinitionKey],
    queryFn: () => getSchemaByProcessKey(task!.processDefinitionKey),
    enabled: !!task?.processDefinitionKey,
    retry: false,
  });

  // Load existing field values (keyed by processInstanceId + taskId)
  const { data: fieldValues } = useQuery({
    queryKey: ["field-values", task?.processInstanceId, id],
    queryFn: () => getFieldValues(task!.processInstanceId, id),
    enabled: !!task?.processInstanceId,
  });

  // Populate form when field values load
  React.useEffect(() => {
    if (!fieldValues) return;
    fieldValues.forEach((fv) => {
      setValue(fv.fieldKey, fv.value);
    });
  }, [fieldValues, setValue]);

  const isAssignedToMe = task?.assignee === user?.id;
  const isCompleted = task?.status === "COMPLETED" || task?.status === "CANCELLED";
  const canClaim = !task?.assignee && !isCompleted;
  const canUnclaim = isAssignedToMe && !isCompleted;
  const canComplete = isAssignedToMe && !isCompleted;
  const canDelegate = isAssignedToMe && !isCompleted;

  const handleFileChange = useCallback(
    async (fieldKey: string, file: File) => {
      try {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("taskId", id!);
        formData.append("fieldKey", fieldKey);

        const { data } = await apiClient.post<{ attachmentId: string }>(
          "/api/v1/attachments",
          formData,
          { headers: { "Content-Type": "multipart/form-data" } }
        );
        setFileUploads((prev) => ({ ...prev, [fieldKey]: data.attachmentId }));
        setValue(fieldKey, data.attachmentId);
      } catch (err) {
        console.error("File upload failed:", err);
      }
    },
    [id, setValue]
  );

  const handleUserSearch = useCallback(async (fieldKey: string, query: string) => {
    if (!query.trim()) {
      setUserSearchResults((prev) => ({ ...prev, [fieldKey]: [] }));
      return;
    }
    try {
      const { data } = await apiClient.get<
        Array<{ id: string; name: string; email: string }>
      >("/api/v1/users/search", { params: { q: query } });
      setUserSearchResults((prev) => ({ ...prev, [fieldKey]: data }));
    } catch {
      setUserSearchResults((prev) => ({ ...prev, [fieldKey]: [] }));
    }
  }, []);

  const handleSelectChange = useCallback(
    (fieldKey: string, value: string) => {
      setSelectValues((prev) => ({ ...prev, [fieldKey]: value }));
      setValue(fieldKey, value);
    },
    [setValue]
  );

  const onSubmit = async (formData: FieldValues) => {
    if (!id || !task || !canComplete) return;
    setSubmitError(null);

    try {
      // Merge file upload references into form values
      const mergedValues: Record<string, unknown> = { ...formData, ...fileUploads };

      // Build typed field value entries for the API
      const schemaFields = schemaData?.fields ?? [];
      const fieldValueEntries = Object.entries(mergedValues).map(([fieldKey, value]) => {
        const fieldDef = schemaFields.find((f) => f.fieldKey === fieldKey);
        return {
          fieldKey,
          fieldType: fieldDef?.fieldType ?? "TEXT",
          value,
        };
      });

      // Save field values
      await saveFieldValues({
        processInstanceId: task.processInstanceId,
        taskId: id,
        values: fieldValueEntries,
      });

      // Complete the task, passing variables for the workflow engine
      await completeTask.mutateAsync({
        id,
        request: { variables: mergedValues },
      });

      navigate("/tasks");
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "Failed to complete task");
    }
  };

  if (taskLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner size="lg" className="text-primary" />
      </div>
    );
  }

  if (taskError || !task) {
    return (
      <PageLayout sidebar={<Sidebar />} header={<Header />}>
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <AlertCircle className="mb-4 h-12 w-12 text-destructive" />
          <h2 className="text-lg font-semibold">Task Not Found</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            The requested task does not exist or you don&apos;t have access.
          </p>
          <Button className="mt-4" variant="outline" onClick={() => navigate("/tasks")}>
            Back to Inbox
          </Button>
        </div>
      </PageLayout>
    );
  }

  const schemaFields: FieldDefinitionDto[] = schemaData?.fields ?? [];

  return (
    <PageLayout sidebar={<Sidebar />} header={<Header />}>
      {/* Back navigation */}
      <div className="mb-4">
        <Button variant="ghost" size="sm" onClick={() => navigate("/tasks")}>
          <ArrowLeft className="h-4 w-4" />
          Back to Inbox
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left: Task info + actions */}
        <div className="space-y-4 lg:col-span-1">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Task Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Task Name
                </p>
                <p className="mt-1 font-medium">{task.name}</p>
              </div>

              {task.description && (
                <div>
                  <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Description
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">{task.description}</p>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Priority
                  </p>
                  <Badge variant={PRIORITY_BADGE[task.priority]} className="mt-1">
                    {task.priority}
                  </Badge>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Status
                  </p>
                  <Badge variant={STATUS_BADGE[task.status]} className="mt-1">
                    {task.status}
                  </Badge>
                </div>
              </div>

              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Process
                </p>
                <a
                  href={`${import.meta.env.VITE_BPMN_DESIGNER_URL ?? "http://localhost:5173"}/processes/${task.processInstanceId}`}
                  className="mt-1 flex items-center gap-1 text-sm text-primary hover:underline"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {task.processDefinitionName || task.processDefinitionKey}
                  <ExternalLink className="h-3 w-3" />
                </a>
              </div>

              {task.dueDate && (
                <div>
                  <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Due Date
                  </p>
                  <p className="mt-1 text-sm">
                    {new Date(task.dueDate).toLocaleString()}
                  </p>
                </div>
              )}

              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Assignee
                </p>
                <p className="mt-1 text-sm">
                  {task.assignee ?? <span className="text-muted-foreground">Unassigned</span>}
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Action buttons */}
          {!isCompleted && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Actions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {canClaim && (
                  <Button
                    className="w-full"
                    onClick={() => claimTask.mutate(task.id)}
                    disabled={claimTask.isPending}
                  >
                    {claimTask.isPending ? <Spinner size="sm" /> : <UserCheck className="h-4 w-4" />}
                    Claim Task
                  </Button>
                )}

                {canUnclaim && (
                  <Button
                    variant="outline"
                    className="w-full"
                    onClick={() => unclaimTask.mutate(task.id)}
                    disabled={unclaimTask.isPending}
                  >
                    {unclaimTask.isPending ? <Spinner size="sm" /> : <UserX className="h-4 w-4" />}
                    Unclaim Task
                  </Button>
                )}

                {canDelegate && (
                  <Button
                    variant="outline"
                    className="w-full"
                    onClick={() => setDelegateDialogOpen(true)}
                  >
                    <UserCog className="h-4 w-4" />
                    Delegate Task
                  </Button>
                )}
              </CardContent>
            </Card>
          )}
        </div>

        {/* Right: Dynamic form */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Task Form</CardTitle>
            </CardHeader>
            <CardContent>
              {schemaLoading && (
                <div className="flex justify-center py-8">
                  <Spinner className="text-primary" />
                </div>
              )}

              {!schemaLoading && schemaFields.length === 0 && (
                <p className="text-sm text-muted-foreground">
                  No form fields defined for this process.
                </p>
              )}

              {!schemaLoading && schemaFields.length > 0 && (
                <form onSubmit={handleSubmit(onSubmit)} noValidate>
                  <DynamicFieldRenderer
                    fields={schemaFields}
                    register={register}
                    errors={errors}
                    disabled={!canComplete}
                    onFileChange={handleFileChange}
                    onUserSearch={handleUserSearch}
                    userSearchResults={userSearchResults}
                    onSelectChange={handleSelectChange}
                    selectValues={selectValues}
                    setValue={setValue}
                  />

                  {submitError && (
                    <div className="mt-4 rounded-md border border-destructive/50 bg-destructive/10 p-3">
                      <p className="text-sm text-destructive">{submitError}</p>
                    </div>
                  )}

                  {canComplete && (
                    <div className="mt-6 flex justify-end">
                      <Button
                        type="submit"
                        disabled={completeTask.isPending}
                        size="lg"
                      >
                        {completeTask.isPending ? (
                          <Spinner size="sm" />
                        ) : (
                          <CheckCircle2 className="h-4 w-4" />
                        )}
                        Complete Task
                      </Button>
                    </div>
                  )}
                </form>
              )}

              {isCompleted && (
                <div className="flex items-center gap-2 rounded-md border border-green-200 bg-green-50 p-4">
                  <CheckCircle2 className="h-5 w-5 text-green-600" />
                  <p className="text-sm text-green-800">
                    This task has been {task.status.toLowerCase()}.
                    {task.completedAt &&
                      ` Completed on ${new Date(task.completedAt).toLocaleString()}.`}
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Delegate Dialog */}
      <Dialog
        open={delegateDialogOpen}
        onOpenChange={(open) => {
          setDelegateDialogOpen(open);
          if (!open) {
            setDelegateUserId("");
            setDelegateComment("");
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delegate Task</DialogTitle>
            <DialogDescription>
              Assign this task to another user. They will be notified.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="delegateUserId">User ID or Email *</Label>
              <Input
                id="delegateUserId"
                placeholder="e.g. user@company.com"
                value={delegateUserId}
                onChange={(e) => setDelegateUserId(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="delegateComment">Comment (optional)</Label>
              <Input
                id="delegateComment"
                placeholder="Reason for delegation..."
                value={delegateComment}
                onChange={(e) => setDelegateComment(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDelegateDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={async () => {
                if (!delegateUserId.trim()) return;
                await delegateTask.mutateAsync({
                  id: task.id,
                  request: {
                    userId: delegateUserId.trim(),
                    comment: delegateComment.trim() || undefined,
                  },
                });
                setDelegateDialogOpen(false);
              }}
              disabled={delegateTask.isPending || !delegateUserId.trim()}
            >
              {delegateTask.isPending ? <Spinner size="sm" /> : <UserCog className="h-4 w-4" />}
              Delegate
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
