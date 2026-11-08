import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ItemDiagram from './ItemDiagram';

const viewer = vi.hoisted(() => ({
  importXML: vi.fn(),
  destroy: vi.fn(),
  zoom: vi.fn(),
  addMarker: vi.fn(),
  elements: new Set<string>(),
}));

vi.mock('../shared-ui', () => ({ apiClient: {} }));
vi.mock('../items/api', () => ({ getWorkflowXml: () => Promise.resolve('<xml/>') }));
vi.mock('bpmn-js/lib/NavigatedViewer', () => ({
  default: function NavigatedViewer() {
    return {
      importXML: viewer.importXML,
      destroy: viewer.destroy,
      get: (service: string) =>
        service === 'canvas'
          ? { zoom: viewer.zoom, addMarker: viewer.addMarker }
          : { get: (id: string) => (viewer.elements.has(id) ? {} : undefined) },
    };
  },
}));

function renderDiagram(statusId: string) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <ItemDiagram versionId="flow:1:1" statusId={statusId} />
    </QueryClientProvider>,
  );
}

describe('ItemDiagram', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    viewer.elements = new Set(['open']);
    viewer.importXML.mockResolvedValue({});
  });

  it('highlights the current status', async () => {
    renderDiagram('open');

    await waitFor(() => expect(viewer.addMarker).toHaveBeenCalledWith('open', 'wfp-current-status'));
  });

  it('highlights nothing when the status is not on the diagram', async () => {
    renderDiagram('END');

    await waitFor(() => expect(viewer.zoom).toHaveBeenCalled());
    expect(viewer.addMarker).not.toHaveBeenCalled();
  });

  it('says so when the workflow has no layout', async () => {
    viewer.importXML.mockRejectedValue(new Error('no diagram to display'));

    renderDiagram('open');

    expect(await screen.findByText('This workflow has no diagram to show.')).toBeTruthy();
  });
});
