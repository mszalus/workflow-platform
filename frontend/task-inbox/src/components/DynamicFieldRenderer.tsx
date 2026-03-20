import { type UseFormRegister, type FieldValues, type FieldErrors } from "react-hook-form";
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, cn } from "@workflow/ui-common";
import type { FieldDefinitionDto } from "@workflow/ui-common";

interface DynamicFieldRendererProps {
  fields: FieldDefinitionDto[];
  register: UseFormRegister<FieldValues>;
  errors: FieldErrors<FieldValues>;
  disabled?: boolean;
  defaultValues?: Record<string, unknown>;
  onFileChange?: (fieldKey: string, file: File) => void;
  onUserSearch?: (fieldKey: string, query: string) => void;
  userSearchResults?: Record<string, Array<{ id: string; name: string; email: string }>>;
  // For controlled select fields (ENUM), supply onChange separately
  onSelectChange?: (fieldKey: string, value: string) => void;
  selectValues?: Record<string, string>;
}

function FieldWrapper({
  field,
  children,
  error,
}: {
  field: FieldDefinitionDto;
  children: React.ReactNode;
  error?: string;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={field.fieldKey} className={cn(field.required && "after:content-['*'] after:ml-0.5 after:text-destructive")}>
        {field.label}
      </Label>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}

export function DynamicFieldRenderer({
  fields,
  register,
  errors,
  disabled = false,
  onFileChange,
  onUserSearch,
  userSearchResults = {},
  onSelectChange,
  selectValues = {},
}: DynamicFieldRendererProps) {
  const sortedFields = [...fields].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <div className="space-y-4">
      {sortedFields.map((field) => {
        const errorMsg = errors[field.fieldKey]?.message as string | undefined;
        const registerOpts = field.required
          ? { required: `${field.label} is required` }
          : {};

        switch (field.fieldType) {
          case "TEXT":
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <Input
                  id={field.fieldKey}
                  type="text"
                  placeholder={field.label}
                  disabled={disabled}
                  {...register(field.fieldKey, registerOpts)}
                />
              </FieldWrapper>
            );

          case "NUMBER":
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <Input
                  id={field.fieldKey}
                  type="number"
                  placeholder={field.label}
                  disabled={disabled}
                  {...register(field.fieldKey, {
                    ...registerOpts,
                    valueAsNumber: true,
                  })}
                />
              </FieldWrapper>
            );

          case "DATE":
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <Input
                  id={field.fieldKey}
                  type="date"
                  disabled={disabled}
                  {...register(field.fieldKey, registerOpts)}
                />
              </FieldWrapper>
            );

          case "BOOLEAN":
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <div className="flex items-center gap-2">
                  <input
                    id={field.fieldKey}
                    type="checkbox"
                    disabled={disabled}
                    className="h-4 w-4 rounded border-input"
                    {...register(field.fieldKey)}
                  />
                  <span className="text-sm text-muted-foreground">
                    Check to enable
                  </span>
                </div>
              </FieldWrapper>
            );

          case "ENUM":
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <Select
                  value={selectValues[field.fieldKey] ?? ""}
                  onValueChange={(value) => onSelectChange?.(field.fieldKey, value)}
                  disabled={disabled}
                >
                  <SelectTrigger id={field.fieldKey}>
                    <SelectValue placeholder={`Select ${field.label}...`} />
                  </SelectTrigger>
                  <SelectContent>
                    {(field.options ?? []).map((option) => (
                      <SelectItem key={option} value={option}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {/* Hidden input to register enum value with react-hook-form */}
                <input
                  type="hidden"
                  {...register(field.fieldKey, registerOpts)}
                  value={selectValues[field.fieldKey] ?? ""}
                />
              </FieldWrapper>
            );

          case "FILE_REF":
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <input
                  id={field.fieldKey}
                  type="file"
                  disabled={disabled}
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) onFileChange?.(field.fieldKey, file);
                  }}
                  className="block w-full text-sm text-muted-foreground file:mr-4 file:rounded-md file:border-0 file:bg-primary file:px-4 file:py-2 file:text-xs file:font-medium file:text-primary-foreground hover:file:bg-primary/90 disabled:opacity-50"
                />
                <p className="text-xs text-muted-foreground">
                  File will be uploaded to the attachment service.
                </p>
              </FieldWrapper>
            );

          case "USER_REF": {
            const results = userSearchResults[field.fieldKey] ?? [];
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <div className="relative">
                  <Input
                    id={`${field.fieldKey}-search`}
                    type="text"
                    placeholder={`Search for ${field.label}...`}
                    disabled={disabled}
                    onChange={(e) => onUserSearch?.(field.fieldKey, e.target.value)}
                  />
                  {results.length > 0 && (
                    <div className="absolute z-10 mt-1 w-full rounded-md border bg-popover shadow-md">
                      {results.map((u) => (
                        <button
                          key={u.id}
                          type="button"
                          className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-accent hover:text-accent-foreground"
                          onClick={() => {
                            // Set hidden field value
                            const hiddenInput = document.getElementById(
                              field.fieldKey
                            ) as HTMLInputElement | null;
                            if (hiddenInput) {
                              hiddenInput.value = u.id;
                              hiddenInput.dispatchEvent(new Event("input", { bubbles: true }));
                            }
                            const searchInput = document.getElementById(
                              `${field.fieldKey}-search`
                            ) as HTMLInputElement | null;
                            if (searchInput) {
                              searchInput.value = `${u.name} (${u.email})`;
                            }
                            onUserSearch?.(field.fieldKey, "");
                          }}
                        >
                          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                            {u.name.charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <p className="font-medium">{u.name}</p>
                            <p className="text-xs text-muted-foreground">{u.email}</p>
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                {/* Hidden input stores the actual user ID */}
                <input
                  id={field.fieldKey}
                  type="hidden"
                  {...register(field.fieldKey, registerOpts)}
                />
              </FieldWrapper>
            );
          }

          default:
            return (
              <FieldWrapper key={field.fieldKey} field={field} error={errorMsg}>
                <Input
                  id={field.fieldKey}
                  type="text"
                  placeholder={field.label}
                  disabled={disabled}
                  {...register(field.fieldKey, registerOpts)}
                />
              </FieldWrapper>
            );
        }
      })}
    </div>
  );
}
