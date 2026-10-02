package com.wfp.workflow.engine.flowable;

import com.wfp.workflow.engine.Node;
import com.wfp.workflow.engine.WorkflowGraph;
import com.wfp.workflow.engine.WorkflowRunListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FlowableRunEvents implements FlowableEventListener {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final FlowableWorkflowEngine engine;
    private final WorkflowRunListener listener;

    @PostConstruct
    void register() {
        runtimeService.addEventListener(this, FlowableEngineEventType.ACTIVITY_STARTED);
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableActivityEvent activity)) {
            return;
        }
        Object itemId = runtimeService.getVariable(activity.getExecutionId(), FlowableWorkflowEngine.ITEM_ID);
        if (itemId == null) {
            return;
        }
        WorkflowGraph graph = engine.graph(activity.getProcessDefinitionId());
        Node node = graph.node(activity.getActivityId()).orElseThrow();
        String tenantId = repositoryService.getProcessDefinition(activity.getProcessDefinitionId()).getTenantId();
        if (graph.isStatus(node)) {
            listener.statusEntered(tenantId, UUID.fromString(itemId.toString()), node.id());
        } else if (graph.endsRun(node)) {
            listener.runEnded(tenantId, UUID.fromString(itemId.toString()), node.name());
        }
    }

    @Override
    public boolean isFailOnException() {
        return true;
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
