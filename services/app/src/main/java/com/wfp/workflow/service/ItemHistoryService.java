package com.wfp.workflow.service;

import com.wfp.common.exception.NotFoundException;
import com.wfp.workflow.dto.AuditEntryDto;
import com.wfp.workflow.dto.ItemHistoryEntry;
import com.wfp.workflow.engine.Status;
import com.wfp.workflow.engine.Transition;
import com.wfp.workflow.engine.WorkflowDescriptor;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.entity.Item;
import com.wfp.workflow.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ItemHistoryService {

    private static final int MAX_AUDIT_ENTRIES = 500;
    private static final String CREATED = "CREATED";
    private static final String STATUS = "STATUS";
    private static final String FIELD = "FIELD";
    private static final String UPGRADE_REASON = "WORKFLOW_UPGRADE";

    private final ItemRepository itemRepository;
    private final AuditService auditService;
    private final WorkflowEngine engine;

    @Transactional(readOnly = true)
    public List<ItemHistoryEntry> history(String key) {
        Item item = itemRepository.findByKey(key).orElseThrow(() -> new NotFoundException("Item", key));
        Map<String, String> transitionNames = transitionNames(engine.describe(item.getWorkflowVersionId()));
        return auditService.query("ITEM", key, null, null, null, null, 0, MAX_AUDIT_ENTRIES).getContent().stream()
                .flatMap(entry -> toHistory(entry, transitionNames))
                .toList();
    }

    private static Stream<ItemHistoryEntry> toHistory(AuditEntryDto entry, Map<String, String> transitionNames) {
        Map<String, Object> details = entry.getDetails();
        return switch (entry.getEventType()) {
            case "item.created" -> Stream.of(row(entry, CREATED, null, null, null, null, null));
            case "item.transitioned" -> {
                Object transition = details.get("transition");
                yield Stream.of(row(entry, STATUS, null, details.get("from"), details.get("to"),
                        transition == null ? null : transitionNames.getOrDefault(transition, transition.toString()),
                        (String) details.get("reason")));
            }
            case "item.workflow.upgraded" -> Stream.of(row(entry, STATUS, null, details.get("fromStatus"),
                    details.get("toStatus"), null, UPGRADE_REASON));
            case "item.updated" -> fieldChanges(details).stream().map(change ->
                    row(entry, FIELD, (String) change.get("field"), change.get("from"), change.get("to"), null, null));
            default -> Stream.empty();
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fieldChanges(Map<String, Object> details) {
        return (List<Map<String, Object>>) details.get("changes");
    }

    private static ItemHistoryEntry row(AuditEntryDto entry, String kind, String field, Object from, Object to,
                                        String transition, String reason) {
        return new ItemHistoryEntry(entry.getTimestamp(), entry.getUserId(), kind, field, from, to, transition, reason);
    }

    private static Map<String, String> transitionNames(WorkflowDescriptor descriptor) {
        return Stream.concat(descriptor.statuses().stream().map(Status::transitions).flatMap(List::stream),
                        descriptor.anyStatusTransitions().stream())
                .collect(Collectors.toMap(Transition::id, Transition::name, (first, duplicate) -> first));
    }
}
