---
title: bpmn-editor
tags:
  - library
  - frontend
type: library
source: frontend/packages/bpmn-editor
---

A thin React wrapper around **bpmn-js**, plus Flowable-specific property panel support.

| File | Role |
|---|---|
| `BpmnEditor.tsx` | The canvas component (import/export XML, palette, modelling) |
| `FlowablePropertiesProvider.ts` | Adds [[Flowable Engine|Flowable]] extension properties to the panel |
| `flowable.json` | The Flowable moddle descriptor |

Consumed only by the [[Admin Portal]] process designer. Build order matters: this package
builds after [[shared-ui]] — see [[Known Pitfalls]].
