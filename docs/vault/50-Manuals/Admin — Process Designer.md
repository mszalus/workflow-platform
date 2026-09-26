---
title: Admin — Process Designer
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Process Designer**

Navigate to **Process Designer** in the sidebar to create BPMN workflow diagrams.

## BPMN Editor

The editor is based on [bpmn-js](https://bpmn.io/toolkit/bpmn-js/) with [[Flowable Engine|Flowable]] extensions. It provides:

- **Visual canvas** — drag-and-drop BPMN elements (tasks, events, gateways, sequence flows)
- **Properties panel** (right sidebar) — configure element properties
- **Palette** (left sidebar) — BPMN element toolbox

## Supported BPMN Elements

| Element        | Description                                    |
|----------------|------------------------------------------------|
| Start Event    | Process entry point                            |
| End Event      | Process exit point                             |
| User Task      | Human task assigned to a user or group         |
| Service Task   | Automated task (Java class, expression, delegate) |
| Exclusive Gateway | Decision point (XOR split/join)             |
| Parallel Gateway  | Parallel split/join                          |
| Sequence Flow  | Connection between elements                    |

## Flowable Properties

The properties panel includes Flowable-specific configuration:

### User Task Properties
| Property         | Description                                         |
|-----------------|-----------------------------------------------------|
| Assignee        | User assigned to the task (e.g., `${initiator}`)    |
| Candidate Users | Comma-separated list of candidate users             |
| Candidate Groups| Comma-separated list of candidate groups            |
| Form Key        | Form identifier for custom rendering                |
| Due Date        | Task deadline expression                            |
| Priority        | Task priority (integer)                             |

### Service Task Properties
| Property            | Description                                     |
|--------------------|-------------------------------------------------|
| Java Class         | Fully qualified class name for JavaDelegate     |
| Expression         | UEL expression (e.g., `${myService.execute()}`) |
| Delegate Expression| Expression resolving to a JavaDelegate          |
| Result Variable    | Variable to store the result                    |

### Async Properties (all activities/gateways/events)
| Property    | Description                                |
|------------|---------------------------------------------|
| Async      | Enable async execution                      |
| Async Before | Execute before the element asynchronously |
| Async After  | Execute after the element asynchronously  |
| Exclusive    | Exclusive async execution (no parallel)   |

## Importing BPMN Files

You can import existing BPMN 2.0 XML files into the visual editor:

1. Click **Import** in the top-right toolbar
2. Select a `.bpmn` or `.bpmn20.xml` file from your computer
3. The diagram renders in the canvas and the process name auto-fills from the filename
4. Review and edit the process in the visual editor
5. Click **Deploy** to deploy it to the Flowable engine

Compatible sources for BPMN files include:
- [Flowable GitHub repo](https://github.com/flowable/flowable-engine) — official samples (Vacation Request, Helpdesk, Review Sales Lead, etc.)
- Any BPMN 2.0 compliant modeler (Camunda Modeler, Signavio, etc.)
- Exported files from this platform's **Export** button

> **Note:** Files using the legacy `activiti:` namespace are supported by Flowable but `flowable:` is recommended. The process must have `isExecutable="true"`.

Sample BPMN files are included in `e2e/samples/` for testing:
- `vacation-request.bpmn20.xml` — multi-step vacation approval with manager review, approval/rejection gateway, and resubmission loop
- `review-sales-lead.bpmn20.xml` — complex workflow with embedded subprocess, parallel gateway, error boundary events, and CRM integration task

## Exporting BPMN Files

Click **Export** to download the current diagram as a `.bpmn` XML file. This preserves all Flowable-specific properties (assignee, candidateGroups, formKey, async settings, etc.).

## Editing Existing Processes

From the Process Definitions list, click **Edit** on any process to load its BPMN XML into the visual editor. You can modify the diagram and redeploy — Flowable auto-increments the version number.

## Deploying from the Designer

1. Create or import your BPMN diagram in the editor
2. Enter a **process name** in the top-right input
3. Click **Deploy**
4. The process is sent to the [[Workflow Service|workflow-service]] and deployed to the Flowable engine
5. You'll be redirected to the Process Definitions list

> **Important:** The BPMN process must have `isExecutable="true"` and a valid process `id` for deployment to succeed.

---


---

**Admin manual** — ← [[Admin — Admin Portal]] · [[Admin — Managing Process Definitions]] →

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Architecture]]
> [[Admin — Admin Portal]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Custom Field Schemas]]
> [[Admin — Audit Log]]
> [[Admin — Keycloak Administration]]
> [[Admin — RabbitMQ Monitoring]]
> [[Admin — Docker Deployment]]
> [[Admin — Kubernetes Deployment]]
> [[Admin — Troubleshooting]]
