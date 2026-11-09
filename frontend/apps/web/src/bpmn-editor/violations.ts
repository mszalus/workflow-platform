export interface Violation {
  elementId: string | null;
  rule: number;
  message: string;
}

export const INVALID_MARKER = 'wfp-invalid';
const OVERLAY_TYPE = 'wfp-violation';

export function showViolations(
  canvas: any,
  overlays: any,
  elementRegistry: any,
  violations: Violation[],
  previouslyMarked: string[],
): string[] {
  previouslyMarked.filter((id) => elementRegistry.get(id)).forEach((id) => canvas.removeMarker(id, INVALID_MARKER));
  overlays.remove({ type: OVERLAY_TYPE });

  const messagesByElement = new Map<string, string[]>();
  const root = canvas.getRootElement();
  violations
    .filter((violation) => violation.elementId && elementRegistry.get(violation.elementId))
    .filter((violation) => elementRegistry.get(violation.elementId) !== root)
    .forEach((violation) => {
      const id = violation.elementId as string;
      messagesByElement.set(id, [...(messagesByElement.get(id) ?? []), violation.message]);
    });

  messagesByElement.forEach((messages, id) => {
    canvas.addMarker(id, INVALID_MARKER);
    overlays.add(id, OVERLAY_TYPE, { position: { top: -10, right: 10 }, html: badge(messages) });
  });
  return [...messagesByElement.keys()];
}

function badge(messages: string[]): HTMLElement {
  const element = document.createElement('div');
  element.className = 'wfp-violation';
  element.textContent = '!';
  element.title = messages.join('\n');
  return element;
}
