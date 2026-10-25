const LOW_PRIORITY = 500;

type Entries = Record<string, unknown>;

const PALETTE_ENTRIES = new Set([
  'hand-tool',
  'lasso-tool',
  'space-tool',
  'global-connect-tool',
  'tool-separator',
  'create.start-event',
  'create.end-event',
  'create.exclusive-gateway',
  'create.subprocess-expanded',
]);

const CONTEXT_PAD_ENTRIES = new Set(['append.end-event', 'append.gateway', 'connect', 'replace', 'delete']);

function keep(entries: Entries, allowed: Set<string>, additions: Entries): Entries {
  const kept = Object.fromEntries(Object.entries(entries).filter(([key]) => allowed.has(key)));
  return { ...kept, ...additions };
}

export function trackerPaletteEntries(entries: Entries, additions: Entries): Entries {
  return keep(entries, PALETTE_ENTRIES, additions);
}

export function trackerContextPadEntries(entries: Entries, additions: Entries): Entries {
  const canAppend = 'append.append-task' in entries;
  return keep(entries, CONTEXT_PAD_ENTRIES, canAppend ? additions : {});
}

function TrackerPaletteProvider(this: any, palette: any, create: any, elementFactory: any, translate: any) {
  const createEntry = (type: string, className: string, title: string) => {
    const start = (event: Event) => create.start(event, elementFactory.createShape({ type }));
    return { group: 'activity', className, title: translate(title), action: { dragstart: start, click: start } };
  };

  this.getPaletteEntries = () => (entries: Entries) =>
    trackerPaletteEntries(entries, {
      'create.user-task': createEntry('bpmn:UserTask', 'bpmn-icon-user-task', 'Create a status (user task)'),
      'create.service-task': createEntry('bpmn:ServiceTask', 'bpmn-icon-service-task', 'Create a service task'),
    });

  palette.registerProvider(LOW_PRIORITY, this);
}

TrackerPaletteProvider.$inject = ['palette', 'create', 'elementFactory', 'translate'];

function TrackerContextPadProvider(this: any, contextPad: any, autoPlace: any, elementFactory: any, translate: any) {
  this.getContextPadEntries = (element: any) => (entries: Entries) =>
    trackerContextPadEntries(entries, {
      'append.user-task': {
        group: 'model',
        className: 'bpmn-icon-user-task',
        title: translate('Append a status (user task)'),
        action: {
          click: () => autoPlace.append(element, elementFactory.createShape({ type: 'bpmn:UserTask' })),
        },
      },
    });

  contextPad.registerProvider(LOW_PRIORITY, this);
}

TrackerContextPadProvider.$inject = ['contextPad', 'autoPlace', 'elementFactory', 'translate'];

export default {
  __init__: ['trackerPaletteProvider', 'trackerContextPadProvider'],
  trackerPaletteProvider: ['type', TrackerPaletteProvider],
  trackerContextPadProvider: ['type', TrackerContextPadProvider],
};
