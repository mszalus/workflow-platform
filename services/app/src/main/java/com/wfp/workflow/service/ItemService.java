package com.wfp.workflow.service;

import com.wfp.common.dto.PagedResponse;
import com.wfp.common.exception.BadRequestException;
import com.wfp.common.exception.NotFoundException;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.CreateItemRequest;
import com.wfp.workflow.dto.ItemDto;
import com.wfp.workflow.engine.Status;
import com.wfp.workflow.engine.StatusCategory;
import com.wfp.workflow.engine.Transition;
import com.wfp.workflow.engine.WorkflowDescriptor;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.engine.WorkflowGraph;
import com.wfp.workflow.engine.WorkflowRunListener;
import com.wfp.workflow.entity.FieldOption;
import com.wfp.workflow.entity.FieldSchema;
import com.wfp.workflow.entity.FieldType;
import com.wfp.workflow.entity.Item;
import com.wfp.workflow.entity.ItemTransition;
import com.wfp.workflow.entity.ItemType;
import com.wfp.workflow.entity.NotificationType;
import com.wfp.workflow.entity.Priority;
import com.wfp.workflow.entity.Project;
import com.wfp.workflow.repository.FieldSchemaRepository;
import com.wfp.workflow.repository.ItemRepository;
import com.wfp.workflow.repository.ItemTransitionRepository;
import com.wfp.workflow.repository.ProjectRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ItemService implements WorkflowRunListener {

    private static final String SYSTEM_ACTOR = "system";
    private static final String ENDED_STATUS_NAME = "Closed";
    private static final String FIELDS = "fields";
    private static final Set<String> EDITABLE = Set.of("title", "description", "priority", "assignee", FIELDS);

    private final ItemRepository itemRepository;
    private final ProjectRepository projectRepository;
    private final ItemTransitionRepository transitionRepository;
    private final FieldSchemaRepository fieldSchemaRepository;
    private final WorkflowEngine engine;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ThreadLocal<Cause> currentCause = new ThreadLocal<>();

    private record Cause(String actor, String transitionId, String reason) {
    }

    @Transactional
    public ItemDto create(CreateItemRequest request, String actor) {
        String tenantId = TenantContext.requireCurrentTenantId();
        Project project = projectRepository.findByKeyForUpdate(request.getProject())
                .orElseThrow(() -> new NotFoundException("Project", request.getProject()));
        ItemType type = project.getItemTypes().stream()
                .filter(candidate -> candidate.getName().equals(request.getType()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Project " + project.getKey() + " has no item type " + request.getType()));
        int number = project.getNextItemNumber();
        project.setNextItemNumber(number + 1);

        Item item = itemRepository.save(Item.builder()
                .tenantId(tenantId)
                .key(project.getKey() + "-" + number)
                .project(project)
                .type(type)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(Objects.requireNonNullElse(request.getPriority(), Priority.MEDIUM))
                .assignee(request.getAssignee())
                .reporter(actor)
                .fields(validFields(type.getWorkflowKey(), request.getFields()))
                .workflowVersionId(engine.latestVersion(tenantId, type.getWorkflowKey()))
                .build());
        withCause(new Cause(actor, null, "CREATED"),
                () -> item.setRunId(engine.start(tenantId, item.getWorkflowVersionId(), item.getId()).runId()));

        auditService.record("item.created", "ITEM", item.getKey(), actor,
                Map.of("title", item.getTitle(), "type", type.getName(), "status", item.getStatusName()));
        notifyAssignee(item, actor);
        return toDto(item, true);
    }

    @Transactional(readOnly = true)
    public ItemDto get(String key) {
        return toDto(requireItem(key), true);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ItemDto> list(String projectKey, String assignee, int page, int size) {
        Specification<Item> spec = (root, query, cb) -> cb.and(Stream.of(
                        projectKey == null ? null : cb.equal(root.get("project").get("key"), projectKey),
                        assignee == null ? null : cb.equal(root.get("assignee"), assignee))
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new));
        Page<Item> items = itemRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PagedResponse.of(items.getContent().stream().map(item -> toDto(item, false)).toList(),
                page, size, items.getTotalElements());
    }

    @Transactional
    public ItemDto update(String key, Map<String, Object> changes, String actor) {
        Set<String> unknown = changes.keySet().stream().filter(field -> !EDITABLE.contains(field))
                .collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new BadRequestException("These fields cannot be changed: " + unknown);
        }
        Item item = requireItem(key);
        List<Map<String, Object>> diff = new ArrayList<>();
        String previousAssignee = item.getAssignee();
        if (changes.containsKey("title")) {
            String title = (String) changes.get("title");
            if (title == null || title.isBlank()) {
                throw new BadRequestException("Title is required");
            }
            apply(diff, "title", item.getTitle(), title, item::setTitle);
        }
        if (changes.containsKey("description")) {
            apply(diff, "description", item.getDescription(), (String) changes.get("description"),
                    item::setDescription);
        }
        if (changes.containsKey("priority")) {
            apply(diff, "priority", item.getPriority(), parsePriority(changes.get("priority")), item::setPriority);
        }
        if (changes.containsKey("assignee")) {
            apply(diff, "assignee", item.getAssignee(), (String) changes.get("assignee"), item::setAssignee);
        }
        if (changes.get(FIELDS) instanceof Map<?, ?> fieldChanges) {
            Map<String, Object> merged = new LinkedHashMap<>(item.getFields());
            fieldChanges.forEach((field, value) -> merged.put(field.toString(), value));
            Map<String, Object> valid = validFields(item.getType().getWorkflowKey(), merged);
            Stream.concat(item.getFields().keySet().stream(), valid.keySet().stream()).distinct()
                    .filter(field -> !Objects.equals(item.getFields().get(field), valid.get(field)))
                    .forEach(field -> diff.add(change(FIELDS + "." + field, item.getFields().get(field),
                            valid.get(field))));
            item.setFields(valid);
        }
        if (!diff.isEmpty()) {
            auditService.record("item.updated", "ITEM", item.getKey(), actor, Map.of("changes", diff));
        }
        if (!Objects.equals(previousAssignee, item.getAssignee())) {
            notifyAssignee(item, actor);
        }
        return toDto(item, true);
    }

    @Transactional
    public ItemDto transition(String key, String transitionId, String reason, String actor) {
        Item item = requireItem(key);
        if (WorkflowGraph.END.equals(item.getStatusKey())) {
            throw new BadRequestException(item.getKey() + " is closed");
        }
        withCause(new Cause(actor, transitionId, reason),
                () -> engine.transition(item.getTenantId(), item.getRunId(), transitionId));
        return toDto(item, true);
    }

    @Override
    @Transactional
    public void statusEntered(String tenantId, UUID itemId, String statusId) {
        TenantContext.runAs(tenantId, () -> {
            Item item = itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item", itemId));
            Status status = engine.describe(item.getWorkflowVersionId()).statuses().stream()
                    .filter(candidate -> candidate.id().equals(statusId))
                    .findFirst()
                    .orElseThrow();
            moveTo(item, status.id(), status.name(), status.category());
        });
    }

    @Override
    @Transactional
    public void runEnded(String tenantId, UUID itemId, String endName) {
        TenantContext.runAs(tenantId, () -> {
            Item item = itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item", itemId));
            String name = endName == null || endName.isBlank() ? ENDED_STATUS_NAME : endName;
            moveTo(item, WorkflowGraph.END, name, StatusCategory.DONE);
        });
    }

    private void moveTo(Item item, String statusKey, String statusName, StatusCategory category) {
        String fromStatus = item.getStatusKey();
        String fromName = item.getStatusName();
        item.setStatusKey(statusKey);
        item.setStatusName(statusName);
        item.setStatusCategory(category);
        Cause cause = Objects.requireNonNullElse(currentCause.get(), new Cause(SYSTEM_ACTOR, null, null));
        transitionRepository.save(ItemTransition.builder()
                .tenantId(item.getTenantId())
                .itemId(item.getId())
                .fromStatus(fromStatus)
                .toStatus(statusKey)
                .transitionId(cause.transitionId())
                .actor(cause.actor())
                .workflowVersionId(item.getWorkflowVersionId())
                .reason(cause.reason())
                .build());
        if (fromStatus == null) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("from", fromName);
        details.put("to", statusName);
        details.put("transition", cause.transitionId());
        details.put("reason", cause.reason());
        auditService.record("item.transitioned", "ITEM", item.getKey(), cause.actor(), details);
        Stream.of(item.getReporter(), item.getAssignee())
                .filter(Objects::nonNull)
                .filter(user -> !user.equals(cause.actor()))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .forEach(user -> notificationService.createNotification(user, item.getTenantId(),
                        item.getKey() + " moved to " + statusName, item.getTitle(),
                        NotificationType.ITEM_TRANSITIONED, item.getKey(), "ITEM"));
    }

    private void withCause(Cause cause, Runnable action) {
        currentCause.set(cause);
        try {
            action.run();
        } finally {
            currentCause.remove();
        }
    }

    private void notifyAssignee(Item item, String actor) {
        if (item.getAssignee() != null && !item.getAssignee().equals(actor)) {
            notificationService.createNotification(item.getAssignee(), item.getTenantId(),
                    item.getKey() + " assigned to you", item.getTitle(),
                    NotificationType.ITEM_ASSIGNED, item.getKey(), "ITEM");
        }
    }

    private Item requireItem(String key) {
        return itemRepository.findByKey(key).orElseThrow(() -> new NotFoundException("Item", key));
    }

    private Map<String, Object> validFields(String workflowKey, Map<String, Object> fields) {
        Map<String, FieldSchema> definitions = fieldSchemaRepository
                .findByProcessDefinitionKeyAndTenantIdOrderBySortOrder(workflowKey,
                        TenantContext.requireCurrentTenantId())
                .stream()
                .collect(Collectors.toMap(FieldSchema::getFieldKey, Function.identity()));
        Map<String, Object> valid = new LinkedHashMap<>();
        Objects.requireNonNullElse(fields, Map.<String, Object>of()).forEach((field, value) -> {
            if (!definitions.containsKey(field)) {
                throw new BadRequestException("Unknown field '" + field + "'");
            }
            if (value != null && !"".equals(value)) {
                valid.put(field, value);
            }
        });
        definitions.values().forEach(definition -> requireValid(definition, valid.get(definition.getFieldKey())));
        return valid;
    }

    private static void requireValid(FieldSchema definition, Object value) {
        String field = definition.getFieldKey();
        if (value == null) {
            if (definition.isRequired()) {
                throw new BadRequestException("Field '" + field + "' is required");
            }
            return;
        }
        if (definition.getFieldType() == FieldType.DROPDOWN && !definition.getOptions().isEmpty()
                && definition.getOptions().stream().map(FieldOption::getValue).noneMatch(value.toString()::equals)) {
            throw new BadRequestException("Field '" + field + "' has no option '" + value + "'");
        }
        if (definition.getValidationRegex() != null && !value.toString().matches(definition.getValidationRegex())) {
            throw new BadRequestException("Field '" + field + "' failed validation");
        }
    }

    private static Priority parsePriority(Object value) {
        try {
            return Priority.valueOf(String.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Priority must be one of LOW, MEDIUM, HIGH");
        }
    }

    private static <T> void apply(List<Map<String, Object>> diff, String field, T current, T next,
                                  Consumer<T> setter) {
        if (!Objects.equals(current, next)) {
            diff.add(change(field, current, next));
            setter.accept(next);
        }
    }

    private static Map<String, Object> change(String field, Object from, Object to) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("from", from);
        change.put("to", to);
        return change;
    }

    private ItemDto toDto(Item item, boolean withTransitions) {
        return ItemDto.builder()
                .key(item.getKey())
                .project(item.getProject().getKey())
                .type(item.getType().getName())
                .title(item.getTitle())
                .description(item.getDescription())
                .priority(item.getPriority())
                .assignee(item.getAssignee())
                .reporter(item.getReporter())
                .statusId(item.getStatusKey())
                .statusName(item.getStatusName())
                .statusCategory(item.getStatusCategory())
                .fields(item.getFields())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .transitions(withTransitions ? availableTransitions(item) : null)
                .build();
    }

    private List<Transition> availableTransitions(Item item) {
        if (WorkflowGraph.END.equals(item.getStatusKey())) {
            return List.of();
        }
        WorkflowDescriptor descriptor = engine.describe(item.getWorkflowVersionId());
        Stream<Transition> fromStatus = descriptor.statuses().stream()
                .filter(status -> status.id().equals(item.getStatusKey()))
                .flatMap(status -> status.transitions().stream());
        return Stream.concat(fromStatus, descriptor.anyStatusTransitions().stream()).toList();
    }
}
