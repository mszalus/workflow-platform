import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import { getWorkflowXml } from '../items/api';
import './ItemDiagram.css';

const CURRENT_STATUS_MARKER = 'wfp-current-status';

export default function ItemDiagram({ versionId, statusId }: { versionId: string; statusId: string }) {
  const container = useRef<HTMLDivElement>(null);
  const [hasNoLayout, setHasNoLayout] = useState(false);
  const { data: xml } = useQuery({
    queryKey: ['workflowXml', versionId],
    queryFn: () => getWorkflowXml(versionId),
  });

  useEffect(() => {
    if (!xml || !container.current) return;
    let stillDisplayed = true;
    const viewer = new NavigatedViewer({ container: container.current });
    viewer
      .importXML(xml)
      .then(() => {
        if (!stillDisplayed) return;
        const canvas = viewer.get<{ zoom: (level: string) => void; addMarker: (id: string, marker: string) => void }>(
          'canvas',
        );
        canvas.zoom('fit-viewport');
        if (viewer.get<{ get: (id: string) => unknown }>('elementRegistry').get(statusId)) {
          canvas.addMarker(statusId, CURRENT_STATUS_MARKER);
        }
      })
      .catch(() => {
        if (stillDisplayed) setHasNoLayout(true);
      });
    return () => {
      stillDisplayed = false;
      viewer.destroy();
    };
  }, [xml, statusId]);

  return (
    <>
      {hasNoLayout && <p style={{ fontSize: '0.85rem' }}>This workflow has no diagram to show.</p>}
      <div
        ref={container}
        aria-label="Workflow diagram"
        style={{ height: 220, background: '#fff', display: hasNoLayout ? 'none' : 'block' }}
      />
    </>
  );
}
