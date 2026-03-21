/// <reference types="vite/client" />

declare module 'bpmn-js-properties-panel';
declare module '@bpmn-io/properties-panel';

declare module 'bpmn-js/lib/Modeler' {
  export default class BpmnModeler {
    constructor(options?: any);
    importXML(xml: string): Promise<{ warnings: any[] }>;
    saveXML(options?: { format?: boolean }): Promise<{ xml?: string }>;
    createDiagram(): Promise<{ warnings: any[] }>;
    on(event: string, callback: (...args: any[]) => void): void;
    destroy(): void;
    get(name: string): any;
  }
}

declare module 'bpmn-js/lib/util/ModelUtil' {
  export function is(element: any, type: string): boolean;
  export function getBusinessObject(element: any): any;
}
