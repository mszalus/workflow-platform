package com.wfp.workflow.service;

import com.wfp.common.exception.NotFoundException;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.UpgradeSummary;
import com.wfp.workflow.engine.ProposedWorkflow;
import com.wfp.workflow.engine.Status;
import com.wfp.workflow.engine.WorkflowDescriptor;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.engine.WorkflowGraph;
import com.wfp.workflow.engine.WorkflowRun;
import com.wfp.workflow.entity.Item;
import com.wfp.workflow.entity.ItemTransition;
import com.wfp.workflow.repository.ItemRepository;
import com.wfp.workflow.repository.ItemTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowUpgradeService {

    private static final String UPGRADE_REASON = "WORKFLOW_UPGRADE";

    private enum Outcome {
        SAME_STATUS, WALKED_BACK, RESET
    }

    private record Target(String statusId, Outcome outcome) {
    }

    private final WorkflowEngine engine;
    private final ItemRepository itemRepository;
    private final ItemTransitionRepository transitionRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public UpgradeSummary preview(String bpmnXml) {
        ProposedWorkflow proposed = engine.inspect(bpmnXml);
        Map<Outcome, Integer> counts = new EnumMap<>(Outcome.class);
        itemRepository.findByTypeWorkflowKeyAndStatusKeyNot(proposed.key(), WorkflowGraph.END)
                .forEach(item -> counts.merge(targetFor(item, proposed.descriptor()).outcome(), 1, Integer::sum));
        return summary(proposed.key(), counts, 0);
    }

    public UpgradeSummary moveOpenItems(String workflowKey, String actor) {
        String versionId = engine.latestVersion(TenantContext.requireCurrentTenantId(), workflowKey);
        WorkflowDescriptor descriptor = engine.describe(versionId);
        Map<Outcome, Integer> counts = new EnumMap<>(Outcome.class);
        int failed = 0;
        for (UUID itemId : openItemsOnOlderVersions(workflowKey, versionId)) {
            try {
                Outcome outcome = transactionTemplate.execute(
                        transaction -> moveItem(itemId, versionId, descriptor, actor));
                counts.merge(outcome, 1, Integer::sum);
            } catch (RuntimeException e) {
                log.warn("Could not move item {} to workflow version {}", itemId, versionId, e);
                failed++;
            }
        }
        return summary(workflowKey, counts, failed);
    }

    private List<UUID> openItemsOnOlderVersions(String workflowKey, String versionId) {
        return Objects.requireNonNull(transactionTemplate.execute(transaction ->
                itemRepository.findByTypeWorkflowKeyAndStatusKeyNot(workflowKey, WorkflowGraph.END).stream()
                        .filter(item -> !versionId.equals(item.getWorkflowVersionId()))
                        .map(Item::getId)
                        .toList()));
    }

    private Outcome moveItem(UUID itemId, String versionId, WorkflowDescriptor descriptor, String actor) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item", itemId));
        Target target = targetFor(item, descriptor);
        Status status = descriptor.statuses().stream()
                .filter(candidate -> candidate.id().equals(target.statusId()))
                .findFirst()
                .orElseThrow();
        String fromStatus = item.getStatusKey();
        String fromName = item.getStatusName();
        String fromVersion = item.getWorkflowVersionId();

        engine.cancel(item.getTenantId(), item.getRunId(), UPGRADE_REASON);
        WorkflowRun run = engine.startAt(item.getTenantId(), versionId, item.getId(), status.id());

        item.setRunId(run.runId());
        item.setWorkflowVersionId(versionId);
        item.setStatusKey(status.id());
        item.setStatusName(status.name());
        item.setStatusCategory(status.category());
        transitionRepository.save(ItemTransition.builder()
                .tenantId(item.getTenantId())
                .itemId(item.getId())
                .fromStatus(fromStatus)
                .toStatus(status.id())
                .actor(actor)
                .workflowVersionId(versionId)
                .reason(UPGRADE_REASON)
                .build());
        auditService.record("item.workflow.upgraded", "ITEM", item.getKey(), actor, Map.of(
                "fromStatus", fromName,
                "toStatus", status.name(),
                "fromVersion", fromVersion,
                "toVersion", versionId,
                "outcome", target.outcome().name()));
        return target.outcome();
    }

    private Target targetFor(Item item, WorkflowDescriptor descriptor) {
        Set<String> statusIds = descriptor.statuses().stream().map(Status::id).collect(Collectors.toSet());
        if (statusIds.contains(item.getStatusKey())) {
            return new Target(item.getStatusKey(), Outcome.SAME_STATUS);
        }
        return transitionRepository.findByItemIdOrderByTransitionedAtAsc(item.getId()).stream()
                .map(ItemTransition::getToStatus)
                .filter(statusIds::contains)
                .findFirst()
                .map(statusId -> new Target(statusId, Outcome.WALKED_BACK))
                .orElseGet(() -> new Target(descriptor.initialStatusId(), Outcome.RESET));
    }

    private static UpgradeSummary summary(String workflowKey, Map<Outcome, Integer> counts, int failed) {
        int sameStatus = counts.getOrDefault(Outcome.SAME_STATUS, 0);
        int walkedBack = counts.getOrDefault(Outcome.WALKED_BACK, 0);
        int reset = counts.getOrDefault(Outcome.RESET, 0);
        return new UpgradeSummary(workflowKey, sameStatus + walkedBack + reset + failed, sameStatus, walkedBack, reset,
                failed);
    }
}
