import React, { useEffect, useRef, useCallback } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';

export interface BpmnEditorProps {
  xml?: string;
  onXmlChange?: (xml: string) => void;
  onError?: (error: Error) => void;
  readOnly?: boolean;
  height?: string | number;
}

export function BpmnEditor({ xml, onXmlChange, onError, readOnly = false, height = '100%' }: BpmnEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const modelerRef = useRef<BpmnModeler | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const modeler = new BpmnModeler({
      container: containerRef.current,
    });

    modelerRef.current = modeler;

    if (xml) {
      modeler.importXML(xml).catch((err: Error) => {
        onError?.(err);
      });
    } else {
      modeler.createDiagram().catch((err: Error) => {
        onError?.(err);
      });
    }

    modeler.on('commandStack.changed', async () => {
      try {
        const result = await modeler.saveXML({ format: true });
        if (result.xml) {
          onXmlChange?.(result.xml);
        }
      } catch (err) {
        onError?.(err as Error);
      }
    });

    return () => {
      modeler.destroy();
    };
  }, []);

  const importXml = useCallback(async (newXml: string) => {
    if (modelerRef.current) {
      await modelerRef.current.importXML(newXml);
    }
  }, []);

  useEffect(() => {
    if (xml && modelerRef.current) {
      importXml(xml);
    }
  }, [xml, importXml]);

  return (
    <div
      ref={containerRef}
      style={{
        height: typeof height === 'number' ? `${height}px` : height,
        width: '100%',
        border: '1px solid #ccc',
      }}
    />
  );
}
