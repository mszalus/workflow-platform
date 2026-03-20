import { useEffect, useRef, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Save, Upload, ArrowLeft, AlertCircle, CheckCircle2 } from "lucide-react";
import { Button, Spinner } from "@workflow/ui-common";
import { getProcessDefinitionXml, deployProcess } from "@/api/workflowApi";

// bpmn-js is a CommonJS package – import dynamically to avoid SSR/type issues
// and to keep the main bundle lean.
type BpmnModeler = {
  importXML: (xml: string) => Promise<{ warnings: string[] }>;
  saveXML: (opts: { format: boolean }) => Promise<{ xml: string }>;
  destroy: () => void;
  attachTo: (container: HTMLElement) => void;
};

const EMPTY_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:endEvent id="EndEvent_1" name="End">
      <bpmn:incoming>Flow_1</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_1" bpmnElement="StartEvent_1">
        <dc:Bounds x="156" y="82" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="432" y="82" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <dc:Waypoint x="192" y="100" />
        <dc:Waypoint x="432" y="100" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

type SaveStatus = "idle" | "saving" | "saved" | "error";
type DeployStatus = "idle" | "deploying" | "deployed" | "error";

export default function BpmnDesignerPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isNew = id === "new";

  const canvasRef = useRef<HTMLDivElement>(null);
  const propertiesPanelRef = useRef<HTMLDivElement>(null);
  const modelerRef = useRef<BpmnModeler | null>(null);

  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const [deployStatus, setDeployStatus] = useState<DeployStatus>("idle");
  const [deploymentName, setDeploymentName] = useState("");
  const [modelerReady, setModelerReady] = useState(false);
  const [initError, setInitError] = useState<string | null>(null);

  // Fetch existing BPMN XML when editing
  const { data: existingXml, isLoading: xmlLoading } = useQuery({
    queryKey: ["process-xml", id],
    queryFn: () => getProcessDefinitionXml(id!),
    enabled: !isNew && !!id,
  });

  // Initialize bpmn-js modeler
  const initModeler = useCallback(async () => {
    if (!canvasRef.current || !propertiesPanelRef.current) return;

    try {
      // Dynamic import so bundler can split the heavy bpmn-js chunk
      const [BpmnModelerModule, { BpmnPropertiesPanelModule, BpmnPropertiesProviderModule }] =
        await Promise.all([
          import("bpmn-js/lib/Modeler"),
          import("bpmn-js-properties-panel"),
        ]);

      const BpmnJS = BpmnModelerModule.default as new (opts: unknown) => BpmnModeler;

      const modeler = new BpmnJS({
        container: canvasRef.current,
        propertiesPanel: {
          parent: propertiesPanelRef.current,
        },
        additionalModules: [BpmnPropertiesPanelModule, BpmnPropertiesProviderModule],
        moddleExtensions: {},
      });

      modelerRef.current = modeler;
      setModelerReady(true);
    } catch (err) {
      console.error("Failed to initialize BPMN modeler:", err);
      setInitError("Failed to load BPMN editor. Please refresh the page.");
    }
  }, []);

  // Boot modeler on mount
  useEffect(() => {
    initModeler();
    return () => {
      modelerRef.current?.destroy();
      modelerRef.current = null;
    };
  }, [initModeler]);

  // Load XML once modeler is ready
  useEffect(() => {
    if (!modelerReady || !modelerRef.current) return;

    const xml = existingXml ?? EMPTY_BPMN;
    modelerRef.current
      .importXML(xml)
      .then(({ warnings }) => {
        if (warnings.length > 0) {
          console.warn("BPMN import warnings:", warnings);
        }
      })
      .catch((err: Error) => {
        console.error("Failed to import BPMN XML:", err);
        setInitError(`Failed to load diagram: ${err.message}`);
      });
  }, [modelerReady, existingXml]);

  const handleSave = useCallback(async () => {
    if (!modelerRef.current) return;
    setSaveStatus("saving");
    try {
      const { xml } = await modelerRef.current.saveXML({ format: true });
      // In a real app, this would PUT to /api/v1/process-definitions/:id
      console.info("Saved BPMN XML:", xml.slice(0, 100));
      setSaveStatus("saved");
      setTimeout(() => setSaveStatus("idle"), 2000);
    } catch (err) {
      console.error("Save failed:", err);
      setSaveStatus("error");
      setTimeout(() => setSaveStatus("idle"), 3000);
    }
  }, []);

  const handleDeploy = useCallback(async () => {
    if (!modelerRef.current) return;
    setDeployStatus("deploying");
    try {
      const { xml } = await modelerRef.current.saveXML({ format: true });
      const name = deploymentName.trim() || "Process Deployment";
      await deployProcess({ name, bpmnXml: xml });
      setDeployStatus("deployed");
      setTimeout(() => setDeployStatus("idle"), 3000);
    } catch (err) {
      console.error("Deploy failed:", err);
      setDeployStatus("error");
      setTimeout(() => setDeployStatus("idle"), 3000);
    }
  }, [deploymentName]);

  if (xmlLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner size="lg" className="text-primary" />
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-background">
      {/* Toolbar */}
      <div className="flex h-12 flex-shrink-0 items-center gap-2 border-b bg-card px-4">
        <Button variant="ghost" size="sm" onClick={() => navigate("/processes")}>
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>

        <div className="mx-2 h-5 w-px bg-border" />

        <span className="text-sm font-medium text-muted-foreground">
          {isNew ? "New Process" : `Editing: ${id}`}
        </span>

        <div className="flex-1" />

        {/* Deployment name input */}
        <input
          type="text"
          placeholder="Deployment name..."
          value={deploymentName}
          onChange={(e) => setDeploymentName(e.target.value)}
          className="h-8 w-52 rounded-md border border-input bg-transparent px-3 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
        />

        <Button
          variant="outline"
          size="sm"
          onClick={handleSave}
          disabled={saveStatus === "saving" || !modelerReady}
        >
          {saveStatus === "saving" ? (
            <Spinner size="sm" />
          ) : saveStatus === "saved" ? (
            <CheckCircle2 className="h-4 w-4 text-green-600" />
          ) : saveStatus === "error" ? (
            <AlertCircle className="h-4 w-4 text-destructive" />
          ) : (
            <Save className="h-4 w-4" />
          )}
          {saveStatus === "saving" ? "Saving..." : saveStatus === "saved" ? "Saved!" : "Save"}
        </Button>

        <Button
          size="sm"
          onClick={handleDeploy}
          disabled={deployStatus === "deploying" || !modelerReady}
        >
          {deployStatus === "deploying" ? (
            <Spinner size="sm" />
          ) : deployStatus === "deployed" ? (
            <CheckCircle2 className="h-4 w-4" />
          ) : deployStatus === "error" ? (
            <AlertCircle className="h-4 w-4" />
          ) : (
            <Upload className="h-4 w-4" />
          )}
          {deployStatus === "deploying"
            ? "Deploying..."
            : deployStatus === "deployed"
            ? "Deployed!"
            : deployStatus === "error"
            ? "Deploy Failed"
            : "Deploy"}
        </Button>
      </div>

      {/* Main area */}
      <div className="flex flex-1 overflow-hidden">
        {/* BPMN Canvas */}
        <div className="relative flex-1 overflow-hidden">
          {initError ? (
            <div className="flex h-full items-center justify-center">
              <div className="max-w-md rounded-lg border border-destructive/50 bg-destructive/10 p-6 text-center">
                <AlertCircle className="mx-auto mb-3 h-8 w-8 text-destructive" />
                <p className="text-sm text-destructive">{initError}</p>
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-4"
                  onClick={() => window.location.reload()}
                >
                  Reload Page
                </Button>
              </div>
            </div>
          ) : (
            <>
              {!modelerReady && (
                <div className="absolute inset-0 z-10 flex items-center justify-center bg-background/80">
                  <div className="flex flex-col items-center gap-3">
                    <Spinner size="lg" className="text-primary" />
                    <p className="text-sm text-muted-foreground">Loading BPMN editor...</p>
                  </div>
                </div>
              )}
              <div
                id="bpmn-canvas"
                ref={canvasRef}
                className="bpmn-container h-full w-full"
              />
            </>
          )}
        </div>

        {/* Properties Panel */}
        <div className="flex w-72 flex-shrink-0 flex-col border-l bg-card">
          <div className="flex h-10 items-center border-b px-3">
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Properties
            </span>
          </div>
          <div
            id="bpmn-properties-panel"
            ref={propertiesPanelRef}
            className="flex-1 overflow-y-auto"
          />
        </div>
      </div>
    </div>
  );
}
