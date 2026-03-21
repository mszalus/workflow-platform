# User Manual — Workflow Platform

## Table of Contents

1. [Getting Started](#getting-started)
2. [Login](#login)
3. [Dashboard](#dashboard)
4. [Task Inbox](#task-inbox)
5. [Completing a Task](#completing-a-task)
6. [Starting a New Process](#starting-a-new-process)
7. [My Processes](#my-processes)
8. [Notifications](#notifications)

---

## Getting Started

The Workflow Platform User Portal is a web application for end users to interact with BPMN workflows. You can:

- View and complete tasks assigned to you
- Start new workflow processes
- Track your running processes
- Receive notifications about task assignments and process completions

**URL:** `http://localhost:5174` (local development)

### Prerequisites

- A user account provisioned in Keycloak (the identity provider)
- A modern web browser (Chrome, Firefox, Edge)

---

## Login

When you navigate to the User Portal, you'll be redirected to the Keycloak login page.

1. Enter your **username** (e.g., `admin-a`) and **password** (default: `password`)
2. Click **Sign In**
3. You'll be redirected back to the User Portal dashboard

> **Tip:** Your session persists until the JWT token expires. If you're logged out unexpectedly, simply log in again.

### Default Users

| Username  | Tenant    | Role  | Password |
|-----------|-----------|-------|----------|
| `admin-a` | tenant-a  | admin | password |
| `admin-b` | tenant-b  | admin | password |

---

## Dashboard

The dashboard is your landing page after login. It shows:

- **My Tasks** — count of tasks currently assigned to you
- **Unread Notifications** — count of unread notifications
- **Start New Process** — quick link to start a workflow

The dashboard gives you a quick overview of your workload at a glance.

---

## Task Inbox

Navigate to **Tasks** in the sidebar to view your task inbox.

The task inbox displays all tasks assigned to you in a table with columns:

| Column    | Description                              |
|-----------|------------------------------------------|
| Task      | Task name (clickable link to task detail) |
| Process   | The process definition this task belongs to |
| Created   | Date the task was created                |
| Priority  | Task priority level                      |
| Actions   | Link to open the task                    |

### Filtering

Tasks are automatically filtered to show only tasks assigned to your username.

---

## Completing a Task

Click on a task name or the **Open** link to view the task detail page.

### Task Detail Page

The task detail page has three sections:

#### Task Info
Displays task metadata:
- **Assignee** — who the task is assigned to
- **Priority** — task priority
- **Created** — when the task was created
- **Due Date** — deadline (if set)

#### Custom Fields
Shows any custom fields configured for this process. Custom fields are dynamic form fields defined by administrators. If no custom fields are configured, you'll see "No custom fields configured."

#### Comments
A comment thread for collaboration:
- View existing comments with author and timestamp
- Add new comments using the text input and **Add** button
- Comments are associated with the process instance, visible to all participants

### Completing

Click the green **Complete Task** button to mark the task as done. You'll be redirected back to the task inbox. The next task in the workflow (if any) will be created automatically.

> **Note:** Completing a task is irreversible. Make sure all required work is done before completing.

---

## Starting a New Process

Navigate to **Start Process** in the sidebar.

You'll see a grid of available process definitions (deployed by administrators). Each card shows:
- **Process name**
- **Process key** (technical identifier)
- **Start** button

Click **Start** to create a new instance of that process. The first task will be created and assigned according to the BPMN definition. You'll be redirected to **My Processes** to track it.

---

## My Processes

Navigate to **My Processes** in the sidebar to see all process instances in your tenant.

The table shows:

| Column       | Description                          |
|--------------|--------------------------------------|
| Process      | Process definition name              |
| Business Key | Optional business identifier         |
| Started      | When the process was started         |
| Status       | Current status (Running / Completed) |

---

## Notifications

Navigate to **Notifications** in the sidebar.

Notifications are generated automatically when:
- A task is **assigned** to you
- A task is **created** in a process you're involved in
- A process you started is **completed**

### Notification List

Each notification shows:
- **Title** — summary of the event
- **Message** — detailed description
- **Timestamp** — when the notification was created
- Visual indicator: **blue highlight** for unread, **white** for read

### Marking as Read

- Click **Mark All Read** to mark all notifications as read
- The unread count on the dashboard updates automatically

---

## Multi-Tenant Isolation

The platform supports multiple tenants (organizations). Each tenant's data is completely isolated:

- You can only see processes, tasks, and notifications belonging to your tenant
- Tenant membership is determined by your Keycloak account
- There is no way to access another tenant's data

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Loading..." stuck on screen | Check that all backend services are running. Verify gateway health at `http://localhost:9080/actuator/health` |
| Redirected to login repeatedly | Your JWT token may have expired. Clear browser cookies and log in again |
| No processes available to start | Ask an administrator to deploy a BPMN process definition |
| Tasks not appearing | Verify you're logged in as the correct user. Tasks are filtered by assignee |
| Notifications not appearing | Notifications are created asynchronously via RabbitMQ. Allow a few seconds for delivery |
