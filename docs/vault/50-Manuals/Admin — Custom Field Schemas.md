---
title: Admin — Custom Field Schemas
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Custom Field Schemas**

Navigate to **Custom Fields** in the sidebar to define dynamic form fields for processes.

Custom fields allow you to attach structured data to process instances without modifying the BPMN definition.

## Creating a Field Schema

1. Select a **process definition** from the dropdown
2. Fill in the field configuration:
   - **Field Key** — unique identifier (e.g., `customer_name`)
   - **Label** — display label (e.g., "Customer Name")
   - **Field Type** — one of: TEXT, TEXTAREA, NUMBER, DATE, DATETIME, BOOLEAN, DROPDOWN, MULTI_SELECT, FILE, USER_PICKER
   - **Required** — whether the field is mandatory
3. Click **Add**

## Managing Fields

The table shows all field schemas for the selected process:

| Column   | Description              |
|----------|--------------------------|
| Key      | Field identifier         |
| Label    | Display label            |
| Type     | Field data type          |
| Required | Yes/No                   |
| Actions  | Delete button            |

## How Custom Fields Work

- Schemas are stored in the [[Custom Fields Service|custom-fields-service]] (separate from the workflow engine)
- Field values are associated with process instances
- The [[User Portal]] displays custom fields on the Task Detail page
- Fields are queried via the gateway: `GET /api/fields/values?processInstanceId=...`

---


---

**Admin manual** — ← [[Admin — Managing Process Definitions]] · [[Admin — Audit Log]] →

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Architecture]]
> [[Admin — Admin Portal]]
> [[Admin — Process Designer]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Audit Log]]
> [[Admin — Keycloak Administration]]
> [[Admin — RabbitMQ Monitoring]]
> [[Admin — Docker Deployment]]
> [[Admin — Kubernetes Deployment]]
> [[Admin — Troubleshooting]]
