import { useState } from 'react';
import type { UpgradeSummary } from '../admin/api';

interface DeployDialogProps {
  summary: UpgradeSummary;
  busy: boolean;
  onCancel: () => void;
  onDeploy: (moveOpenItems: boolean) => void;
}

export default function DeployDialog({ summary, busy, onCancel, onDeploy }: DeployDialogProps) {
  const [moveOpenItems, setMoveOpenItems] = useState(false);

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 20,
      }}
    >
      <div
        role="dialog"
        aria-label={`Deploy ${summary.workflowKey}`}
        style={{ background: '#fff', padding: '1.5rem', borderRadius: 4, width: 460 }}
      >
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Deploy {summary.workflowKey}</h2>
        <p>{summary.openItems} open items use this workflow.</p>
        <label style={{ display: 'block', marginBottom: '0.5rem' }}>
          <input type="radio" name="openItems" checked={!moveOpenItems} onChange={() => setMoveOpenItems(false)} /> Keep
          them on their current version
        </label>
        <label style={{ display: 'block', marginBottom: '0.5rem' }}>
          <input type="radio" name="openItems" checked={moveOpenItems} onChange={() => setMoveOpenItems(true)} /> Move
          them to the new version
        </label>
        <p style={{ fontSize: '0.85rem', color: '#666' }}>
          Dry run: same status {summary.sameStatus} · walked back {summary.walkedBack} · reset {summary.reset}
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
          <button onClick={onCancel}>Cancel</button>
          <button disabled={busy} onClick={() => onDeploy(moveOpenItems)}>
            {busy ? 'Deploying...' : 'Deploy'}
          </button>
        </div>
      </div>
    </div>
  );
}
