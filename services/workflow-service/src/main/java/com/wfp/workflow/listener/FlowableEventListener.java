package com.wfp.workflow.listener;

import com.wfp.events.EventConstants;
import com.wfp.events.TaskAssignedEvent;
import com.wfp.events.TaskCreatedEvent;
import com.wfp.workflow.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowableEventListener implements org.flowable.common.engine.api.delegate.event.FlowableEventListener {

    private final EventPublisher eventPublisher;

    @Override
    public void onEvent(FlowableEvent event) {
        FlowableEventType type = event.getType();
        String typeName = type.name();

        if ("TASK_CREATED".equals(typeName)) {
            handleTaskCreated(event);
        } else if ("TASK_ASSIGNED".equals(typeName)) {
            handleTaskAssigned(event);
        } else {
            log.debug("Unhandled Flowable event: {}", typeName);
        }
    }

    private void handleTaskCreated(FlowableEvent event) {
        if (event instanceof FlowableEngineEntityEvent entityEvent
                && entityEvent.getEntity() instanceof TaskEntity task) {
            TaskCreatedEvent e = TaskCreatedEvent.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .taskDefinitionKey(task.getTaskDefinitionKey())
                    .processInstanceId(task.getProcessInstanceId())
                    .assignee(task.getAssignee())
                    .priority(task.getPriority())
                    .build();
            e.initDefaults(EventConstants.TASK_CREATED, task.getTenantId(), task.getAssignee());
            eventPublisher.publish(EventConstants.TASK_CREATED, e);
        }
    }

    private void handleTaskAssigned(FlowableEvent event) {
        if (event instanceof FlowableEngineEntityEvent entityEvent
                && entityEvent.getEntity() instanceof TaskEntity task) {
            TaskAssignedEvent e = TaskAssignedEvent.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .processInstanceId(task.getProcessInstanceId())
                    .assignee(task.getAssignee())
                    .build();
            e.initDefaults(EventConstants.TASK_ASSIGNED, task.getTenantId(), task.getAssignee());
            eventPublisher.publish(EventConstants.TASK_ASSIGNED, e);
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
