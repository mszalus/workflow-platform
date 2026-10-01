# Tracker Screens

Status: **decisions S-1 to S-9 approved on 2026-10-01** (S-2 changed, S-5 moved to the backlog). The wireframes are still under review. Behaviour behind the screens is in [work-item-tracker.md](work-item-tracker.md); the architecture is in [tracker-architecture.md](tracker-architecture.md).

## Decisions to approve

| # | Question | Proposal | Alternative | Decision (2026-10-01) |
|---|---|---|---|---|
| S-1 | One app or two portals | **One app.** An *Admin* section appears only for the `admin` role | Keep `admin-portal` and `user-portal` | Approved |
| S-2 | Board columns | **The status categories** (proposed: To do, In progress, Done). The item's own status shows as a badge on the card. This works across item types that use different workflows | One column per workflow status, for boards filtered to one item type | **Changed:** four columns, with *Open* before *To do*: Open, To do, In progress, Done. The status categories become `OPEN`, `TODO`, `IN_PROGRESS`, `DONE` |
| S-3 | Drag and drop on the board | **Yes.** Dropping on a column runs the transition into that category. If several transitions fit, a picker asks which one; if none fits, the drop is refused | Buttons on the item only | Approved |
| S-4 | Item types | **Defined by the admin per project** (for example Task, Bug), each mapped to one workflow | A fixed global list | Approved |
| S-5 | Attachments | **Not in the first version.** The `wf_attachments` table exists, but there is no API | Upload and download on the item | Approved; attachments go to the backlog (#74) |
| S-6 | Workflow diagram on the item | **Yes, read-only.** A small bpmn-js viewer highlights the current status | Leave it out | Approved |
| S-7 | Notifications | **Bell with a dropdown, polled every 30 s.** No separate page | Real-time push (SSE), which needs new infrastructure | Approved |
| S-8 | Users and groups | **Managed in the Keycloak admin console**, linked from Admin | Our own user screens | Approved |
| S-9 | Dashboards and reports | **Not in the first version** | A project summary page | Approved |

## Navigation

```
+----------------------+-------------------------------------------------------------+
| WFP        [PROJ v]  |  <page title>                        [search] [bell 3] [+ Create] |
|                      |                                                             |
|  My work             |                                                             |
|  Items               |                                                             |
|  Board               |                                                             |
|                      |                                                             |
|  ADMIN (admin only)  |                                                             |
|  Projects            |                                                             |
|  Workflows           |                                                             |
|  Fields              |                                                             |
|  Audit log           |                                                             |
|                      |                                                             |
|  user-a · tenant-a   |                                                             |
|  [Log out]           |                                                             |
+----------------------+-------------------------------------------------------------+
```

- `[PROJ v]` switches the current project. Items, Board and Create work inside it.
- `[+ Create]` opens the Create item dialog (screen 5) from any page.

## User screens

### 1. My work (home)

Items assigned to me across all projects, grouped by status category. It replaces both dashboards and the task inbox.

```
My work
+----------------------------------------------------------------------------------+
| IN PROGRESS (2)                                                                  |
|  PROJ-12  Fix login redirect       Bug    In Review     High    updated 2h ago  |
|  OPS-3    Rotate DB credentials    Task   Doing         Medium  updated 1d ago  |
| TO DO (1)                                                                        |
|  PROJ-15  Add CSV export           Story  Ready         Low     updated 3d ago  |
| OPEN (1)                                                                         |
|  PROJ-17  Crash when saving draft  Bug    New           High    updated 1h ago  |
| DONE, last 7 days (4)                                                [show all] |
+----------------------------------------------------------------------------------+
Recently updated items I reported                                        [show all]
  PROJ-9   Timeout on board load     Bug    Done        closed 1d ago
```

### 2. Items (list with filters)

The structured filter builder from 20.e: each row is field / operator / value, and every filter becomes one SQL query over fixed and `jsonb` fields.

```
Items · PROJ
+----------------------------------------------------------------------------------+
| [Status category v] [is      v] [Open, To do, In progress v]                [x] |
| [Assignee        v] [is      v] [me                 v]                      [x] |
| [Severity (cf)   v] [>=      v] [High               v]                      [x] |
| [+ Add filter]                                            [Clear]  [Apply]      |
+----------------------------------------------------------------------------------+
| Key v    | Title                   | Type  | Status    | Assignee | Priority | Updated |
|----------|-------------------------|-------|-----------|----------|----------|---------|
| PROJ-15  | Add CSV export          | Story | Ready     | user-a   | Low      | 3d ago  |
| PROJ-12  | Fix login redirect      | Bug   | In Review | user-a   | High     | 2h ago  |
| ...                                                                              |
|                                              < 1 2 3 >     25 per page [v]      |
+----------------------------------------------------------------------------------+
```

- Clicking a column header sorts, including by a custom field.
- `(cf)` marks custom fields in the field picker.

### 3. Board

```
Board · PROJ                                         [Type: All v] [Assignee: All v]
+-------------------+-------------------+-------------------+-------------------+
| OPEN (2)          | TO DO (3)         | IN PROGRESS (2)   | DONE (5)          |
| +---------------+ | +---------------+ | +---------------+ | +---------------+ |
| | PROJ-17   Bug | | | PROJ-15 Story | | | PROJ-12   Bug | | | PROJ-9    Bug | |
| | Crash when    | | | Add CSV export| | | Fix login     | | | Timeout on    | |
| | saving draft  | | |               | | | redirect      | | | board load    | |
| | [New]      -- | | | [Ready]user-a | | | [In Review]   | | | [Done] user-b | |
| +---------------+ | +---------------+ | +---------------+ | +---------------+ |
| +---------------+ | +---------------+ | +---------------+ |                   |
| | PROJ-18 ...   | | | PROJ-16 ...   | | | ...           | |                   |
+-------------------+-------------------+-------------------+-------------------+

Drop PROJ-15 on IN PROGRESS, when two transitions lead there:
        +------------------------------------+
        | Move PROJ-15 to In progress        |
        |  ( ) Start work   -> Doing         |
        |  ( ) Triage       -> In Triage     |
        |                 [Cancel] [Move]    |
        +------------------------------------+
```

`[New]`, `[Ready]` and `[In Review]` are the item's actual status (its BPMN user task); the column is its category. A new item starts in the workflow's first status, which is usually in *Open*.

### 4. Item detail

```
PROJ-12 · Bug                                                        [... More v]
Fix login redirect
[In Review]   Transitions: [Approve]  [Request changes]  [Cancel]
+----------------------------------------------------+-----------------------------+
| Description                                  [Edit] | Assignee   user-a     [v]  |
| After login the user lands on /dashboard           | Reporter   user-b           |
| instead of the page they asked for.                | Priority   High       [v]  |
|                                                    | Created    2026-09-28       |
| Comments                                           | Updated    2h ago           |
| +------------------------------------------------+ | -- Custom fields --         |
| | user-b · 1d ago                                | | Severity   High       [v]  |
| | Reproduced on Chrome and Firefox.              | | Component  Auth       [v]  |
| +------------------------------------------------+ | Due date   2026-10-05 [..] |
| [Add a comment...                     ] [Post]     |                             |
|                                                    | Workflow  (bug-flow v3)     |
| History  (later phase, #75)                        |  (O)->[Open]->[Doing]->     |
|  2h ago  user-a  Doing -> In Review (Submit)       |   [*In Review*]->[Done]     |
|  1d ago  user-a  Priority Medium -> High           |                             |
|  1d ago  user-b  created                           |                             |
+----------------------------------------------------+-----------------------------+
```

- The transition buttons are the named outgoing flows of the current status (20.1). A transition may ask for a reason when the model requires one.
- History merges status transitions (`wf_item_transition`) with field changes. **The panel comes in a later phase (#75)**, together with attachments (#74). The first version records the history but doesn't show it.

### 5. Create item (dialog)

```
+-------------------------------------------------------------+
| Create item                                                 |
| Project     [PROJ - Platform          v]                    |
| Type        [Bug                       v]                   |
| Title       [                                          ]    |
| Description [                                          ]    |
|             [                                          ]    |
| Assignee    [unassigned v]     Priority [Medium v]          |
| -- Fields for Bug in PROJ --                                |
| Severity *  [      v]          Component [      v]          |
|                                        [Cancel] [Create]    |
+-------------------------------------------------------------+
```

Custom fields come from the definitions for this project and item type. A `*` marks a required field.

### 6. Notifications (bell)

```
                                   [bell 3]
                     +---------------------------------------+
                     | PROJ-12 assigned to you     · 2h ago  |
                     | PROJ-9  moved to Done       · 1d ago  |
                     | OPS-3   comment from user-b · 1d ago  |
                     |                    [Mark all as read] |
                     +---------------------------------------+
```

Clicking an entry opens the item and marks the entry as read.

## Admin screens (role `admin`)

### 7. Projects

```
Projects                                                         [+ New project]
+-------+------------------+--------------------------------------+--------------+
| Key   | Name             | Item types -> workflow               | Open items   |
|-------|------------------|--------------------------------------|--------------|
| PROJ  | Platform         | Bug -> bug-flow v3, Task -> simple v1| 24           |
| OPS   | Operations       | Task -> simple v1                    | 7            |
+-------+------------------+--------------------------------------+--------------+

Edit project PROJ
  Key [PROJ]  (fixed after the first item)      Name [Platform              ]
  Item types
   [Bug  ] uses workflow [bug-flow v]   [remove]
   [Task ] uses workflow [simple   v]   [remove]
   [+ Add item type]
                                                              [Cancel] [Save]
```

### 8. Workflows

```
Workflows                                                  [Import BPMN] [+ New]
+------------+--------------+---------+----------------------------+--------------+
| Key        | Name         | Latest  | Open items per version     | Used by      |
|------------|--------------|---------|----------------------------|--------------|
| bug-flow   | Bug workflow | v3      | v3: 18, v2: 4              | PROJ/Bug     |
| simple     | Simple task  | v1      | v1: 9                      | PROJ, OPS    |
+------------+--------------+---------+----------------------------+--------------+
Row actions: Edit · Export BPMN · Versions
```

### 9. Workflow editor

The bpmn-js editor with the tracker profile guardrails from 20.3.

```
bug-flow (draft of v4)                    [Validate] [Export] [Save draft] [Deploy]
+-----------+-------------------------------------------+-------------------------+
| Palette   |                                           | Properties              |
| (only the |   (O)-->[Open]-->< >--Start-->[Doing]     | User task "In Review"   |
| profile's |              |    \--Reject->[Closed](X)  | Status category         |
| elements) |              v                            |  [In progress v]        |
|  (O) ( )  |          [In Review] !                    | Candidate groups        |
|  [ ] < >  |                                           |  [reviewers        ]    |
|  [+]      |                                           |                         |
+-----------+-------------------------------------------+-------------------------+
| Problems (2)                                                                    |
|  ! In Review: no outgoing transition, so an end event is not reachable (rule 5) |
|  ! Gateway after Open: flow to Closed has no name (rule 4)                      |
+----------------------------------------------------------------------------------+
```

- Clicking a problem selects the element.
- *Save draft* is allowed with errors; *Deploy* is not.

### 10. Deploy dialog

The version move from 20.4, with a dry run.

```
+----------------------------------------------------------------+
| Deploy bug-flow v4                                             |
| 22 open items run on older versions.                           |
|  (o) Keep them on their current version (default)              |
|  ( ) Move them to v4                                           |
|      Dry run:  same status 17 · walked back 4 · reset 1        |
|      [Show affected items]                                     |
|                                        [Cancel] [Deploy]       |
+----------------------------------------------------------------+
```

### 11. Fields

Custom field definitions, scoped by project and item type. This replaces today's Custom Fields page.

```
Fields · PROJ                                                        [+ New field]
+-------------+-----------+----------+-----------------------+----------+--------+
| Name        | Key       | Type     | Item types            | Required | Filter |
|-------------|-----------|----------|-----------------------|----------|--------|
| Severity    | severity  | Select   | Bug                   | yes      | yes    |
| Component   | component | Select   | Bug, Task             | no       | yes    |
| Due date    | due_date  | Date     | all                   | no       | yes    |
+-------------+-----------+----------+-----------------------+----------+--------+
```

### 12. Audit log

```
Audit log                          [Who: any v] [What: any v] [From .. To ..] [Apply]
+------------------+---------+----------------------------------------------------+
| When             | Who     | What                                               |
|------------------|---------|----------------------------------------------------|
| 2026-09-30 10:12 | admin-a | Deployed bug-flow v4 (moved 22 items)              |
| 2026-09-30 09:40 | admin-a | Changed field Severity: added option "Critical"    |
| 2026-09-29 17:03 | user-a  | PROJ-12 Doing -> In Review                         |
+------------------+---------+----------------------------------------------------+
```

## What happens to today's screens

| Today | App | Becomes |
|---|---|---|
| Dashboard | both | My work (1) |
| My Tasks, Task detail | user | My work (1), Item detail (4) |
| Start Process | user | Create item (5) |
| My Processes | user | Items, filtered by reporter = me (2) |
| Notifications page | user | Bell (6) |
| Processes, Process Designer | admin | Workflows (8), Workflow editor (9), Deploy dialog (10) |
| Custom Fields | admin | Fields (11) |
| Audit Log | admin | Audit log (12) |
| — | — | New: Items (2), Board (3), Projects (7) |
