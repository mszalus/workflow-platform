package com.workflowplatform.engine.service;

import com.workflowplatform.engine.dto.WorkflowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEventPublisher {

    private final KafkaTemplate<String, WorkflowEvent> kafkaTemplate;

    @Value("${kafka.topics.workflow-events:workflow.events}")
    private String workflowEventsTopic;

    public void publish(WorkflowEvent event) {
        // Use processInstanceId or eventId as partition key for ordering guarantees
        String key = event.getProcessInstanceId() != null
            ? event.getProcessInstanceId()
            : event.getEventId();

        CompletableFuture<SendResult<String, WorkflowEvent>> future =
            kafkaTemplate.send(workflowEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event [{}] type=[{}] to topic=[{}]",
                    event.getEventId(), event.getEventType(), workflowEventsTopic, ex);
            } else {
                log.debug("Published event [{}] type=[{}] partition=[{}] offset=[{}]",
                    event.getEventId(), event.getEventType(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }

    public void publishProcessInstanceStarted(String processInstanceId,
                                              String processDefinitionId,
                                              String processDefinitionKey,
                                              String businessKey,
                                              String tenantId,
                                              String actorId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.PROCESS_INSTANCE_STARTED)
            .processInstanceId(processInstanceId)
            .processDefinitionId(processDefinitionId)
            .processDefinitionKey(processDefinitionKey)
            .businessKey(businessKey)
            .tenantId(tenantId)
            .actorId(actorId)
            .build());
    }

    public void publishProcessInstanceCompleted(String processInstanceId,
                                                String processDefinitionKey,
                                                String tenantId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.PROCESS_INSTANCE_COMPLETED)
            .processInstanceId(processInstanceId)
            .processDefinitionKey(processDefinitionKey)
            .tenantId(tenantId)
            .build());
    }

    public void publishProcessInstanceCancelled(String processInstanceId,
                                                String tenantId,
                                                String actorId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.PROCESS_INSTANCE_CANCELLED)
            .processInstanceId(processInstanceId)
            .tenantId(tenantId)
            .actorId(actorId)
            .build());
    }

    public void publishProcessInstanceSuspended(String processInstanceId,
                                                String tenantId,
                                                String actorId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.PROCESS_INSTANCE_SUSPENDED)
            .processInstanceId(processInstanceId)
            .tenantId(tenantId)
            .actorId(actorId)
            .build());
    }

    public void publishProcessInstanceActivated(String processInstanceId,
                                                String tenantId,
                                                String actorId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.PROCESS_INSTANCE_ACTIVATED)
            .processInstanceId(processInstanceId)
            .tenantId(tenantId)
            .actorId(actorId)
            .build());
    }

    public void publishTaskCreated(String taskId,
                                   String taskName,
                                   String processInstanceId,
                                   String assignee,
                                   String tenantId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.TASK_CREATED)
            .taskId(taskId)
            .taskName(taskName)
            .processInstanceId(processInstanceId)
            .assignee(assignee)
            .tenantId(tenantId)
            .build());
    }

    public void publishTaskAssigned(String taskId,
                                    String taskName,
                                    String processInstanceId,
                                    String assignee,
                                    String tenantId,
                                    String actorId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.TASK_ASSIGNED)
            .taskId(taskId)
            .taskName(taskName)
            .processInstanceId(processInstanceId)
            .assignee(assignee)
            .tenantId(tenantId)
            .actorId(actorId)
            .build());
    }

    public void publishTaskClaimed(String taskId,
                                   String processInstanceId,
                                   String assignee,
                                   String tenantId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.TASK_CLAIMED)
            .taskId(taskId)
            .processInstanceId(processInstanceId)
            .assignee(assignee)
            .tenantId(tenantId)
            .actorId(assignee)
            .build());
    }

    public void publishTaskCompleted(String taskId,
                                     String taskName,
                                     String processInstanceId,
                                     String tenantId,
                                     String actorId,
                                     Map<String, Object> variables) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.TASK_COMPLETED)
            .taskId(taskId)
            .taskName(taskName)
            .processInstanceId(processInstanceId)
            .tenantId(tenantId)
            .actorId(actorId)
            .payload(variables)
            .build());
    }

    public void publishTaskDelegated(String taskId,
                                     String processInstanceId,
                                     String delegatee,
                                     String tenantId,
                                     String actorId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.TASK_DELEGATED)
            .taskId(taskId)
            .processInstanceId(processInstanceId)
            .assignee(delegatee)
            .tenantId(tenantId)
            .actorId(actorId)
            .build());
    }

    public void publishDeploymentCreated(String deploymentId,
                                         String deploymentName,
                                         String tenantId,
                                         String actorId) {
        publish(WorkflowEvent.builder()
            .eventType(WorkflowEvent.DEPLOYMENT_CREATED)
            .tenantId(tenantId)
            .actorId(actorId)
            .payload(Map.of("deploymentId", deploymentId, "deploymentName", deploymentName))
            .build());
    }
}
