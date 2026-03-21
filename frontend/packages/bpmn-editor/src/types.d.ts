declare module '*.json' {
  const value: any;
  export default value;
}

declare module '*.css' {
  const content: string;
  export default content;
}

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

declare module 'bpmn-js-properties-panel' {
  export const BpmnPropertiesPanelModule: any;
  export const BpmnPropertiesProviderModule: any;
  export function useService(name: string, strict?: boolean): any;
}

declare module '@bpmn-io/properties-panel' {
  export function TextFieldEntry(props: any): any;
  export function CheckboxEntry(props: any): any;
  export function SelectEntry(props: any): any;
  export function TextAreaEntry(props: any): any;
  export const isTextFieldEntryEdited: (node: any) => boolean;
  export const isCheckboxEntryEdited: (node: any) => boolean;
  export const isSelectEntryEdited: (node: any) => boolean;
}
