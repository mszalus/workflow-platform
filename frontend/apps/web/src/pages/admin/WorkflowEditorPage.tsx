import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../shared-ui';
import { BpmnEditor, type Violation } from '../../bpmn-editor';
import type { AxiosError } from 'axios';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { downloadBpmn, listWorkflows, moveOpenItems, previewUpgrade, type UpgradeSummary } from '../../admin/api';
import DeployDialog from '../../components/DeployDialog';
import { getWorkflowXml } from '../../items/api';

const VALIDATE_DELAY_MS = 500;

export default function WorkflowEditorPage() {
  const { id } = useParams<{ id: string }>();
  const template = useLocation().state as { xml: string; name: string } | null;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const fileInput = useRef<HTMLInputElement>(null);
  const [xml, setXml] = useState<string | undefined>(template?.xml);
  const [typedName, setTypedName] = useState<string | undefined>(template?.name);
  const [violations, setViolations] = useState<Violation[]>([]);
  const [selectRequest, setSelectRequest] = useState<{ elementId: string }>();
  const [validationError, setValidationError] = useState<string>();
  const [deployError, setDeployError] = useState<string>();
  const [pendingUpgrade, setPendingUpgrade] = useState<UpgradeSummary>();

  const { data: workflows = [] } = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows, enabled: !!id });
  const { data: savedXml } = useQuery({
    queryKey: ['workflowXml', id],
    queryFn: () => getWorkflowXml(id!),
    enabled: !!id,
  });
  const name = typedName ?? workflows.find((workflow) => workflow.id === id)?.key ?? '';

  useEffect(() => {
    if (savedXml) setXml(savedXml);
  }, [savedXml]);

  useEffect(() => {
    if (!xml) return;
    let stale = false;
    const timer = setTimeout(() => {
      apiClient
        .post<Violation[]>('/workflow/workflows/validate', { bpmnXml: xml })
        .then((response) => {
          if (stale) return;
          setViolations(response.data);
          setValidationError(undefined);
        })
        .catch((error: AxiosError) => {
          if (!stale) setValidationError(error.message);
        });
    }, VALIDATE_DELAY_MS);
    return () => {
      stale = true;
      clearTimeout(timer);
    };
  }, [xml]);

  const deploy = useMutation({
    mutationFn: async (moveItems: boolean) => {
      await apiClient.post('/workflow/deployments', { name, bpmnXml: xml });
      return moveItems && pendingUpgrade ? moveOpenItems(pendingUpgrade.workflowKey) : undefined;
    },
    onMutate: () => setDeployError(undefined),
    onSuccess: (moved) => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
      queryClient.invalidateQueries({ queryKey: ['items'] });
      setPendingUpgrade(undefined);
      if (moved?.failed) {
        setDeployError(`Deployed, but ${moved.failed} items could not be moved.`);
      } else {
        navigate('/admin/workflows');
      }
    },
    onError: (error: AxiosError<{ message?: string; details?: { violations?: Violation[] } }>) => {
      const rejected = error.response?.data?.details?.violations;
      if (rejected) setViolations(rejected);
      setPendingUpgrade(undefined);
      setDeployError(error.response?.data?.message ?? error.message);
    },
  });

  const startDeploy = async () => {
    const summary = await previewUpgrade(xml!).catch(() => undefined);
    if (summary && summary.openItems > 0) {
      setPendingUpgrade(summary);
    } else {
      deploy.mutate(false);
    }
  };

  const importFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      setXml(reader.result as string);
      if (!name) setTypedName(file.name.replace(/\.(bpmn20\.xml|bpmn|xml)$/i, ''));
    };
    reader.readAsText(file);
    event.target.value = '';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 6rem)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0, flex: 1 }}>Workflow editor</h1>
        <input
          ref={fileInput}
          aria-label="Import file"
          type="file"
          accept=".bpmn,.xml,.bpmn20.xml"
          onChange={importFile}
          style={{ display: 'none' }}
        />
        <button onClick={() => fileInput.current?.click()}>Import BPMN</button>
        <button disabled={!xml} onClick={() => xml && downloadBpmn(name, xml)}>
          Export
        </button>
        <input
          aria-label="Workflow name"
          placeholder="Workflow name"
          value={name}
          onChange={(event) => setTypedName(event.target.value)}
        />
        <button disabled={!xml || !name || deploy.isPending} onClick={startDeploy}>
          {deploy.isPending ? 'Deploying...' : 'Deploy'}
        </button>
      </div>
      <div style={{ flex: 1, minHeight: 0, background: '#fff' }}>
        <BpmnEditor xml={xml} onXmlChange={setXml} violations={violations} selectRequest={selectRequest} />
      </div>
      <div style={{ maxHeight: '25vh', overflowY: 'auto', padding: '0.5rem 0' }}>
        {deployError && <div role="alert">Deploy failed: {deployError}</div>}
        {validationError && (
          <div role="alert">
            Couldn't check the workflow ({validationError}); the problems below may be out of date.
          </div>
        )}
        <strong>Problems ({violations.length})</strong>
        <ul style={{ margin: '0.25rem 0 0', paddingLeft: '1.25rem' }}>
          {violations.map((violation, index) => (
            <li key={`${violation.elementId}-${violation.rule}-${index}`}>
              <button
                onClick={() => violation.elementId && setSelectRequest({ elementId: violation.elementId })}
                style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', textAlign: 'left' }}
              >
                {violation.message}{' '}
                {violation.rule > 0 && <span style={{ color: '#777' }}>(rule {violation.rule})</span>}
              </button>
            </li>
          ))}
        </ul>
      </div>
      {pendingUpgrade && (
        <DeployDialog
          summary={pendingUpgrade}
          busy={deploy.isPending}
          onCancel={() => setPendingUpgrade(undefined)}
          onDeploy={(moveItems) => deploy.mutate(moveItems)}
        />
      )}
    </div>
  );
}
