import { describe, expect, it } from 'vitest';
import { trackerContextPadEntries, trackerPaletteEntries } from './trackerPalette';

const entry = {};

describe('trackerPaletteEntries', () => {
  it('keeps the tools and the profile elements, and adds the explicit tasks', () => {
    const defaults = Object.fromEntries(
      [
        'hand-tool',
        'lasso-tool',
        'space-tool',
        'global-connect-tool',
        'tool-separator',
        'create.start-event',
        'create.intermediate-event',
        'create.end-event',
        'create.exclusive-gateway',
        'create.task',
        'create.data-object',
        'create.data-store',
        'create.subprocess-expanded',
        'create.participant-expanded',
        'create.group',
      ].map((key) => [key, entry]),
    );

    const entries = trackerPaletteEntries(defaults, { 'create.user-task': entry, 'create.service-task': entry });

    expect(Object.keys(entries).sort()).toEqual(
      [
        'hand-tool',
        'lasso-tool',
        'space-tool',
        'global-connect-tool',
        'tool-separator',
        'create.start-event',
        'create.end-event',
        'create.exclusive-gateway',
        'create.subprocess-expanded',
        'create.user-task',
        'create.service-task',
      ].sort(),
    );
  });
});

describe('trackerContextPadEntries', () => {
  it('drops off-profile appends and appends a user task instead of a generic task', () => {
    const defaults = Object.fromEntries(
      [
        'append.end-event',
        'append.gateway',
        'append.append-task',
        'append.intermediate-event',
        'append.text-annotation',
        'append.timer-intermediate-event',
        'connect',
        'replace',
        'delete',
      ].map((key) => [key, entry]),
    );

    const entries = trackerContextPadEntries(defaults, { 'append.user-task': entry });

    expect(Object.keys(entries).sort()).toEqual(
      ['append.end-event', 'append.gateway', 'append.user-task', 'connect', 'replace', 'delete'].sort(),
    );
  });

  it('offers no append on elements that cannot be followed by a task', () => {
    const entries = trackerContextPadEntries({ replace: entry, delete: entry }, { 'append.user-task': entry });

    expect(Object.keys(entries).sort()).toEqual(['delete', 'replace']);
  });
});
