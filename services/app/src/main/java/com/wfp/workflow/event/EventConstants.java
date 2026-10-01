package com.wfp.workflow.event;

public final class EventConstants {

    private EventConstants() {}

    public static final String PROCESS_STARTED = "process.started";
    public static final String PROCESS_COMPLETED = "process.completed";
    public static final String PROCESS_CANCELLED = "process.cancelled";
    public static final String PROCESS_SLA_BREACHED = "process.sla.breached";

    public static final String TASK_CREATED = "task.created";
    public static final String TASK_ASSIGNED = "task.assigned";
    public static final String TASK_COMPLETED = "task.completed";
    public static final String TASK_DELEGATED = "task.delegated";

    public static final String FIELD_SCHEMA_CREATED = "field.schema.created";
    public static final String FIELD_VALUE_SAVED = "field.value.saved";
}
