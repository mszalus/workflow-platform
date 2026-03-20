import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { PlusCircle, Trash2, GripVertical, Save } from "lucide-react";
import {
  Button,
  Badge,
  PageLayout,
  PageHeader,
  Spinner,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workflow/ui-common";
import type { FieldDefinitionDto, FieldType } from "@workflow/ui-common";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";
import { listProcessDefinitions } from "@/api/workflowApi";
import {
  getSchemaByProcessKey,
  createSchema,
  updateSchema,
} from "@/api/schemaApi";

const FIELD_TYPES: { value: FieldType; label: string }[] = [
  { value: "TEXT", label: "Text" },
  { value: "NUMBER", label: "Number" },
  { value: "DATE", label: "Date" },
  { value: "BOOLEAN", label: "Boolean" },
  { value: "ENUM", label: "Enum (dropdown)" },
  { value: "FILE_REF", label: "File Reference" },
  { value: "USER_REF", label: "User Reference" },
];

interface FieldFormState {
  fieldKey: string;
  label: string;
  fieldType: FieldType;
  required: boolean;
  displayOrder: number;
  options: string;
}

const defaultFieldForm = (): FieldFormState => ({
  fieldKey: "",
  label: "",
  fieldType: "TEXT",
  required: false,
  displayOrder: 0,
  options: "",
});

export default function SchemaBuilderPage() {
  const queryClient = useQueryClient();
  const [selectedProcessKey, setSelectedProcessKey] = useState<string>("");
  const [fields, setFields] = useState<FieldDefinitionDto[]>([]);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [fieldForm, setFieldForm] = useState<FieldFormState>(defaultFieldForm());
  const [formError, setFormError] = useState<string | null>(null);
  const [schemaId, setSchemaId] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const { data: processesData } = useQuery({
    queryKey: ["process-definitions"],
    queryFn: () => listProcessDefinitions({ size: 100 }),
  });

  const { data: schemaData, isLoading: schemaLoading } = useQuery({
    queryKey: ["schema", selectedProcessKey],
    queryFn: () => getSchemaByProcessKey(selectedProcessKey),
    enabled: !!selectedProcessKey,
    retry: false,
  });

  useEffect(() => {
    if (schemaData) {
      setFields([...schemaData.fields].sort((a, b) => a.displayOrder - b.displayOrder));
      setSchemaId(schemaData.id);
    } else if (!schemaLoading) {
      setFields([]);
      setSchemaId(null);
    }
  }, [schemaData, schemaLoading]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const fieldPayload = fields.map(({ id: _id, ...rest }) => rest);
      if (schemaId) {
        return updateSchema(schemaId, { fields: fieldPayload });
      }
      return createSchema({ processDefinitionKey: selectedProcessKey, fields: fieldPayload });
    },
    onSuccess: (result) => {
      setSchemaId(result.id);
      queryClient.invalidateQueries({ queryKey: ["schema", selectedProcessKey] });
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 2000);
    },
  });

  const handleAddField = () => {
    if (!fieldForm.fieldKey.trim() || !fieldForm.label.trim()) {
      setFormError("Field key and label are required");
      return;
    }
    if (fields.some((f) => f.fieldKey === fieldForm.fieldKey.trim())) {
      setFormError("Field key must be unique");
      return;
    }

    const newField: FieldDefinitionDto = {
      id: crypto.randomUUID(),
      fieldKey: fieldForm.fieldKey.trim(),
      label: fieldForm.label.trim(),
      fieldType: fieldForm.fieldType,
      required: fieldForm.required,
      displayOrder: fieldForm.displayOrder || fields.length + 1,
      options:
        fieldForm.fieldType === "ENUM" && fieldForm.options
          ? fieldForm.options.split(",").map((o) => o.trim()).filter(Boolean)
          : undefined,
    };

    setFields((prev) => [...prev, newField]);
    setAddDialogOpen(false);
    setFieldForm(defaultFieldForm());
    setFormError(null);
  };

  const handleRemoveField = (id: string) => {
    setFields((prev) => prev.filter((f) => f.id !== id));
  };

  const handleMoveField = (index: number, direction: "up" | "down") => {
    setFields((prev) => {
      const next = [...prev];
      const targetIndex = direction === "up" ? index - 1 : index + 1;
      if (targetIndex < 0 || targetIndex >= next.length) return prev;
      [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
      return next.map((f, i) => ({ ...f, displayOrder: i + 1 }));
    });
  };

  return (
    <PageLayout sidebar={<Sidebar />} header={<Header />}>
      <PageHeader
        title="Schema Builder"
        description="Define form fields for each process"
        actions={
          <Button
            size="sm"
            onClick={() => saveMutation.mutate()}
            disabled={saveMutation.isPending || !selectedProcessKey || fields.length === 0}
          >
            {saveMutation.isPending ? (
              <Spinner size="sm" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {saveSuccess ? "Saved!" : "Save Schema"}
          </Button>
        }
      />

      {/* Process selector */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">Select Process</CardTitle>
        </CardHeader>
        <CardContent>
          <Select value={selectedProcessKey} onValueChange={setSelectedProcessKey}>
            <SelectTrigger className="w-80">
              <SelectValue placeholder="Choose a process definition..." />
            </SelectTrigger>
            <SelectContent>
              {processesData?.content.map((proc) => (
                <SelectItem key={proc.key} value={proc.key}>
                  {proc.name || proc.key}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {selectedProcessKey && (
        <>
          {schemaLoading && (
            <div className="flex justify-center py-8">
              <Spinner size="lg" className="text-primary" />
            </div>
          )}

          {!schemaLoading && (
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle className="text-base">
                  Field Definitions
                  <Badge variant="secondary" className="ml-2">
                    {fields.length} field{fields.length !== 1 ? "s" : ""}
                  </Badge>
                </CardTitle>
                <Button size="sm" variant="outline" onClick={() => setAddDialogOpen(true)}>
                  <PlusCircle className="h-4 w-4" />
                  Add Field
                </Button>
              </CardHeader>
              <CardContent>
                {fields.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-12 text-center">
                    <p className="text-sm text-muted-foreground">
                      No fields defined yet. Click &ldquo;Add Field&rdquo; to get started.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {fields.map((field, index) => (
                      <div
                        key={field.id}
                        className="flex items-center gap-3 rounded-md border bg-background p-3"
                      >
                        <GripVertical className="h-4 w-4 text-muted-foreground" />

                        <div className="flex flex-1 items-center gap-4">
                          <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2">
                              <span className="font-medium text-sm">{field.label}</span>
                              {field.required && (
                                <Badge variant="destructive" className="text-xs">Required</Badge>
                              )}
                            </div>
                            <div className="mt-0.5 flex items-center gap-2">
                              <code className="text-xs text-muted-foreground">{field.fieldKey}</code>
                              <span className="text-muted-foreground">·</span>
                              <Badge variant="outline" className="text-xs">
                                {field.fieldType}
                              </Badge>
                            </div>
                            {field.options && field.options.length > 0 && (
                              <p className="mt-1 text-xs text-muted-foreground">
                                Options: {field.options.join(", ")}
                              </p>
                            )}
                          </div>

                          <div className="flex items-center gap-1">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7"
                              disabled={index === 0}
                              onClick={() => handleMoveField(index, "up")}
                              title="Move up"
                            >
                              ↑
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7"
                              disabled={index === fields.length - 1}
                              onClick={() => handleMoveField(index, "down")}
                              title="Move down"
                            >
                              ↓
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7 text-destructive hover:text-destructive"
                              onClick={() => handleRemoveField(field.id)}
                              title="Remove field"
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </>
      )}

      {/* Add Field Dialog */}
      <Dialog open={addDialogOpen} onOpenChange={(open) => { setAddDialogOpen(open); if (!open) { setFieldForm(defaultFieldForm()); setFormError(null); } }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add Field</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="fieldKey">Field Key *</Label>
                <Input
                  id="fieldKey"
                  placeholder="e.g. customerName"
                  value={fieldForm.fieldKey}
                  onChange={(e) => setFieldForm((f) => ({ ...f, fieldKey: e.target.value }))}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="fieldLabel">Label *</Label>
                <Input
                  id="fieldLabel"
                  placeholder="e.g. Customer Name"
                  value={fieldForm.label}
                  onChange={(e) => setFieldForm((f) => ({ ...f, label: e.target.value }))}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Field Type</Label>
                <Select
                  value={fieldForm.fieldType}
                  onValueChange={(v) => setFieldForm((f) => ({ ...f, fieldType: v as FieldType }))}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {FIELD_TYPES.map((ft) => (
                      <SelectItem key={ft.value} value={ft.value}>
                        {ft.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="displayOrder">Display Order</Label>
                <Input
                  id="displayOrder"
                  type="number"
                  min={1}
                  value={fieldForm.displayOrder || ""}
                  onChange={(e) =>
                    setFieldForm((f) => ({ ...f, displayOrder: parseInt(e.target.value) || 0 }))
                  }
                />
              </div>
            </div>

            {fieldForm.fieldType === "ENUM" && (
              <div className="space-y-2">
                <Label htmlFor="options">Options (comma-separated)</Label>
                <Input
                  id="options"
                  placeholder="e.g. Option A, Option B, Option C"
                  value={fieldForm.options}
                  onChange={(e) => setFieldForm((f) => ({ ...f, options: e.target.value }))}
                />
              </div>
            )}

            <div className="flex items-center gap-2">
              <input
                id="required"
                type="checkbox"
                checked={fieldForm.required}
                onChange={(e) => setFieldForm((f) => ({ ...f, required: e.target.checked }))}
                className="h-4 w-4 rounded border-input"
              />
              <Label htmlFor="required" className="cursor-pointer font-normal">
                Required field
              </Label>
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setAddDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleAddField}>
              <PlusCircle className="h-4 w-4" />
              Add Field
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
