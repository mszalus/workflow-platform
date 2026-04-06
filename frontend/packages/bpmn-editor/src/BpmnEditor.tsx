import React, { useEffect, useRef, useCallback } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import { BpmnPropertiesPanelModule, BpmnPropertiesProviderModule } from 'bpmn-js-properties-panel';
import flowableModdle from './flowable.json';
import FlowablePropertiesProviderModule from './FlowablePropertiesProvider';

import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import '@bpmn-io/properties-panel/assets/properties-panel.css';

const DEFAULT_DIAGRAM = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:flowable="http://flowable.org/bpmn"
  targetNamespace="http://flowable.org/bpmn">
  <process id="Process_1" name="New Process" isExecutable="true">
    <startEvent id="StartEvent_1" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`;

export interface BpmnEditorProps {
  xml?: string;
  onXmlChange?: (xml: string) => void;
  onError?: (error: Error) => void;
  readOnly?: boolean;
  height?: string | number;
}

export function BpmnEditor({ xml, onXmlChange, onError, readOnly = false, height = '100%' }: BpmnEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const propertiesPanelRef = useRef<HTMLDivElement>(null);
  const modelerRef = useRef<BpmnModeler | null>(null);

  useEffect(() => {
    if (!containerRef.current || !propertiesPanelRef.current) return;

    const modeler = new BpmnModeler({
      container: containerRef.current,
      propertiesPanel: {
        parent: propertiesPanelRef.current,
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule,
        FlowablePropertiesProviderModule,
      ],
      moddleExtensions: {
        flowable: flowableModdle,
      },
    });

    modelerRef.current = modeler;

    const initialXml = xml || DEFAULT_DIAGRAM;
    modeler.importXML(initialXml).then(() => {
      if (!xml) {
        modeler.saveXML({ format: true }).then((result) => {
          if (result.xml) onXmlChange?.(result.xml);
        });
      }
    }).catch((err: Error) => {
      onError?.(err);
    });

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
    <div style={{ display: 'flex', height: typeof height === 'number' ? `${height}px` : height, width: '100%' }}>
      <div
        ref={containerRef}
        style={{
          flex: 1,
          border: '1px solid #ccc',
        }}
      />
      <div
        ref={propertiesPanelRef}
        style={{
          width: 320,
          borderLeft: '1px solid #ccc',
          overflowY: 'auto',
          background: '#f8f8f8',
        }}
      />
    </div>
  );
}
