import { describe, expect, it, vi } from 'vitest';
import { INVALID_MARKER, showViolations } from './violations';

function fakeModeler(existingIds: string[]) {
  return {
    canvas: { addMarker: vi.fn(), removeMarker: vi.fn() },
    overlays: { add: vi.fn(), remove: vi.fn() },
    elementRegistry: { get: (id: string) => (existingIds.includes(id) ? { id } : undefined) },
  };
}

describe('showViolations', () => {
  it('marks each invalid element once, with all of its messages in the badge', () => {
    const { canvas, overlays, elementRegistry } = fakeModeler(['open', 'skip']);

    const marked = showViolations(
      canvas,
      overlays,
      elementRegistry,
      [
        { elementId: 'open', rule: 3, message: 'Status Open needs a wfp:statusCategory' },
        { elementId: 'open', rule: 6, message: 'A status has exactly one outgoing flow' },
        { elementId: 'skip', rule: 4, message: 'Transition needs a name' },
      ],
      [],
    );

    expect(marked).toEqual(['open', 'skip']);
    expect(canvas.addMarker).toHaveBeenCalledTimes(2);
    expect(canvas.addMarker).toHaveBeenCalledWith('open', INVALID_MARKER);
    const badge = overlays.add.mock.calls.find(([id]) => id === 'open')?.[2].html as HTMLElement;
    expect(badge.title).toBe('Status Open needs a wfp:statusCategory\nA status has exactly one outgoing flow');
  });

  it('skips violations without an element or for elements no longer on the canvas', () => {
    const { canvas, overlays, elementRegistry } = fakeModeler(['open']);

    const marked = showViolations(
      canvas,
      overlays,
      elementRegistry,
      [
        { elementId: null, rule: 0, message: 'The workflow is not readable BPMN XML' },
        { elementId: 'deleted', rule: 5, message: 'Unreachable' },
      ],
      [],
    );

    expect(marked).toEqual([]);
    expect(canvas.addMarker).not.toHaveBeenCalled();
    expect(overlays.add).not.toHaveBeenCalled();
  });

  it('clears the previous markers and badges before showing new ones', () => {
    const { canvas, overlays, elementRegistry } = fakeModeler(['open']);

    showViolations(canvas, overlays, elementRegistry, [], ['open', 'deleted']);

    expect(canvas.removeMarker).toHaveBeenCalledOnce();
    expect(canvas.removeMarker).toHaveBeenCalledWith('open', INVALID_MARKER);
    expect(overlays.remove).toHaveBeenCalledWith({ type: 'wfp-violation' });
  });
});
