---
title: Admin — Keycloak Administration
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Keycloak Administration**

[[Security and JWT|Keycloak]] manages authentication and [[Multi-Tenancy|tenant isolation]].

**Admin Console:** `http://localhost:8180/admin`
**Realm:** `workflow-platform`

## Realm Structure

- **Realm:** `workflow-platform` — single realm for all tenants
- **Clients:**
  - `wfp-admin-portal` — OIDC client for [[Admin Portal|admin portal]]
  - `wfp-user-portal` — OIDC client for [[User Portal|user portal]]
- **Client Scopes:**
  - `tenant` — adds `tenant_id` claim to JWT
  - `profile` — adds `preferred_username`, `given_name`, `family_name`
  - `email` — adds `email` claim
  - `roles` — adds `realm_access.roles` claim

## Managing Users

1. Navigate to Keycloak admin console → Users
2. Click **Add User**
3. Set username, email, first/last name
4. Under **Credentials**, set a password
5. Under **Attributes**, add `tenant_id` attribute with the tenant value (e.g., `tenant-a`)

## Tenant Isolation

Tenant isolation is enforced through the `tenant_id` JWT claim:

1. Each user has a `tenant_id` attribute in Keycloak
2. The `tenant` client scope maps this attribute to the JWT `tenant_id` claim
3. The gateway extracts the claim and adds `X-Tenant-Id` header
4. Each service reads the header and filters all database queries by tenant

## Default Configuration

| User      | Tenant   | Attributes                        |
|-----------|----------|-----------------------------------|
| `admin-a` | tenant-a | `tenant_id=tenant-a`             |
| `admin-b` | tenant-b | `tenant_id=tenant-b`             |

---


---

**Admin manual** — ← [[Admin — Audit Log]] · [[Admin — RabbitMQ Monitoring]] →

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Architecture]]
> [[Admin — Admin Portal]]
> [[Admin — Process Designer]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Custom Field Schemas]]
> [[Admin — Audit Log]]
> [[Admin — RabbitMQ Monitoring]]
> [[Admin — Docker Deployment]]
> [[Admin — Kubernetes Deployment]]
> [[Admin — Troubleshooting]]
