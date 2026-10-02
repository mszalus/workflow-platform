package com.wfp.workflow.engine.flowable;

import com.wfp.common.exception.BadRequestException;
import com.wfp.common.exception.NotFoundException;
import com.wfp.workflow.engine.Status;
import com.wfp.workflow.engine.Transition;
import com.wfp.workflow.engine.WorkflowDescriber;
import com.wfp.workflow.engine.WorkflowDescriptor;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.engine.WorkflowGraph;
import com.wfp.workflow.engine.WorkflowRun;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class FlowableWorkflowEngine implements WorkflowEngine {

    static final String ITEM_ID = "itemId";
    private static final String TRANSITION = "transition";

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final org.flowable.engine.TaskService taskService;
    private final FlowableWorkflowParser parser = new FlowableWorkflowParser();
    private final WorkflowDescriber describer = new WorkflowDescriber();
    private final Map<String, WorkflowGraph> graphs = new ConcurrentHashMap<>();
    private final Map<String, WorkflowDescriptor> descriptors = new ConcurrentHashMap<>();

    @Override
    public String latestVersion(String tenantId, String workflowKey) {
        ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(workflowKey)
                .processDefinitionTenantId(tenantId)
                .latestVersion()
                .singleResult();
        if (latest == null) {
            throw new NotFoundException("Workflow", workflowKey);
        }
        return latest.getId();
    }

    @Override
    public WorkflowDescriptor describe(String versionId) {
        return descriptors.computeIfAbsent(versionId, id -> {
            try {
                return describer.describe(graph(id));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Workflow version " + id + " is not a tracker workflow: "
                        + e.getMessage());
            }
        });
    }

    @Override
    public WorkflowRun start(String tenantId, String versionId, UUID itemId) {
        if (!tenantId.equals(repositoryService.getProcessDefinition(versionId).getTenantId())) {
            throw new NotFoundException("Workflow version", versionId);
        }
        ProcessInstance run = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(versionId)
                .variable(ITEM_ID, itemId.toString())
                .start();
        return new WorkflowRun(run.getId(), versionId);
    }

    @Override
    public void transition(String tenantId, String runId, String transitionId) {
        ProcessInstance run = runtimeService.createProcessInstanceQuery()
                .processInstanceId(runId)
                .processInstanceTenantId(tenantId)
                .singleResult();
        if (run == null) {
            throw new NotFoundException("Workflow run", runId);
        }
        WorkflowDescriptor descriptor = describe(run.getProcessDefinitionId());
        Optional<Status> current = currentStatus(runId, descriptor);
        if (current.isPresent() && offers(current.get().transitions(), transitionId)) {
            completeStatusTask(runId, current.get(), transitionId);
        } else if (offers(descriptor.anyStatusTransitions(), transitionId)) {
            triggerEventSubprocess(runId, transitionId);
        } else {
            throw new BadRequestException("Transition '" + transitionId + "' is not available in status "
                    + current.map(Status::name).orElse("(none)"));
        }
    }

    WorkflowGraph graph(String versionId) {
        return graphs.computeIfAbsent(versionId, this::parseVersion);
    }

    private WorkflowGraph parseVersion(String versionId) {
        ProcessDefinition version = repositoryService.getProcessDefinition(versionId);
        try (InputStream xml = repositoryService.getResourceAsStream(version.getDeploymentId(),
                version.getResourceName())) {
            return parser.parse(new String(xml.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<Status> currentStatus(String runId, WorkflowDescriptor descriptor) {
        List<String> activeIds = runtimeService.getActiveActivityIds(runId);
        return descriptor.statuses().stream().filter(status -> activeIds.contains(status.id())).findFirst();
    }

    private static boolean offers(List<Transition> transitions, String transitionId) {
        return transitions.stream().anyMatch(transition -> transition.id().equals(transitionId));
    }

    private void completeStatusTask(String runId, Status status, String transitionId) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(runId)
                .taskDefinitionKey(status.id())
                .singleResult();
        if (task == null) {
            throw new BadRequestException(
                    "Status " + status.name() + " is completed by its steps, not by a transition");
        }
        taskService.complete(task.getId(), Map.of(), Map.of(TRANSITION, transitionId));
    }

    private void triggerEventSubprocess(String runId, String startEventId) {
        EventSubscription subscription = runtimeService.createEventSubscriptionQuery()
                .processInstanceId(runId)
                .activityId(startEventId)
                .singleResult();
        if (subscription == null) {
            throw new BadRequestException("Transition '" + startEventId + "' is not available now");
        }
        runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());
    }
}
