package com.workflowplatform.engine.event;

import com.workflowplatform.engine.domain.TenantContext;
import com.workflowplatform.engine.service.WorkflowEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableProcessEngineEvent;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowFlowableEventListener implements FlowableEventListener {

    private final WorkflowEventPublisher eventPublisher;

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event.getType() instanceof FlowableEngineEventType engineEventType)) {
            return;
        }

        try {
            switch (engineEventType) {
                case PROCESS_STARTED -> handleProcessStarted(event);
                case PROCESS_COMPLETED -> handleProcessCompleted(event);
                case PROCESS_CANCELLED -> handleProcessCancelled(event);
                case TASK_CREATED -> handleTaskCreated(event);
                case TASK_ASSIGNED -> handleTaskAssigned(event);
                case TASK_COMPLETED -> handleTaskCompleted(event);
                default -> { /* not handled */ }
            }
        } catch (Exception e) {
            log.error("Error processing Flowable event type={}", engineEventType, e);
        }
    }

    private void handleProcessStarted(FlowableEvent event) {
        if (event instanceof FlowableProcessStartedEvent processEvent) {
            var execution = (DelegateExecution) processEvent.getEntity();
            if (execution != null) {
                eventPublisher.publishProcessInstanceStarted(
                    execution.getProcessInstanceId(),
                    execution.getProcessDefinitionId(),
                    null,
                    execution.getProcessInstanceBusinessKey(),
                    execution.getTenantId(),
                    TenantContext.getTenantId()
                );
            }
        }
    }

    private void handleProcessCompleted(FlowableEvent event) {
        if (event instanceof FlowableProcessEngineEvent processEvent) {
            eventPublisher.publishProcessInstanceCompleted(
                processEvent.getProcessInstanceId(),
                null,
                TenantContext.getTenantId()
            );
        }
    }

    private void handleProcessCancelled(FlowableEvent event) {
        if (event instanceof FlowableProcessEngineEvent processEvent) {
            eventPublisher.publishProcessInstanceCancelled(
                processEvent.getProcessInstanceId(),
                TenantContext.getTenantId(),
                null
            );
        }
    }

    private void handleTaskCreated(FlowableEvent event) {
        if (event instanceof FlowableEntityEvent entityEvent
                && entityEvent.getEntity() instanceof Task task) {
            eventPublisher.publishTaskCreated(
                task.getId(),
                task.getName(),
                task.getProcessInstanceId(),
                task.getAssignee(),
                task.getTenantId()
            );
        }
    }

    private void handleTaskAssigned(FlowableEvent event) {
        if (event instanceof FlowableEntityEvent entityEvent
                && entityEvent.getEntity() instanceof Task task) {
            eventPublisher.publishTaskAssigned(
                task.getId(),
                task.getName(),
                task.getProcessInstanceId(),
                task.getAssignee(),
                task.getTenantId(),
                null
            );
        }
    }

    private void handleTaskCompleted(FlowableEvent event) {
        if (event instanceof FlowableEntityEvent entityEvent
                && entityEvent.getEntity() instanceof Task task) {
            eventPublisher.publishTaskCompleted(
                task.getId(),
                task.getName(),
                task.getProcessInstanceId(),
                task.getTenantId(),
                null,
                null
            );
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
