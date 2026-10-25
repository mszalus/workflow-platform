package com.wfp.workflow.service;

import com.wfp.common.exception.BadRequestException;
import com.wfp.workflow.dto.ItemFilter;
import com.wfp.workflow.engine.StatusCategory;
import com.wfp.workflow.entity.Item;
import com.wfp.workflow.entity.Priority;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class ItemFilterSpecification {

    private static final String CURRENT_USER = "me";
    private static final String JSON_TEXT = "jsonb_extract_path_text";

    private static final Map<String, Function<Root<Item>, Path<?>>> FIXED_FIELDS = Map.of(
            "project", root -> root.get("project").get("key"),
            "type", root -> root.get("type").get("name"),
            "key", root -> root.get("key"),
            "status", root -> root.get("statusKey"),
            "statusCategory", root -> root.get("statusCategory"),
            "priority", root -> root.get("priority"),
            "assignee", root -> root.get("assignee"),
            "reporter", root -> root.get("reporter"),
            "createdAt", root -> root.get("createdAt"),
            "updatedAt", root -> root.get("updatedAt"));

    private ItemFilterSpecification() {
    }

    static Specification<Item> of(ItemFilter filter, String currentUser) {
        return (root, query, cb) -> filter.isCustomField()
                ? customFieldPredicate(filter, root, cb)
                : fixedFieldPredicate(filter, root, cb, currentUser);
    }

    private static Predicate customFieldPredicate(ItemFilter filter, Root<Item> root, CriteriaBuilder cb) {
        Expression<String> value = cb.function(JSON_TEXT, String.class,
                root.get("fields"), cb.literal(filter.customFieldKey()));
        return switch (filter.operator()) {
            case EQ -> cb.equal(value, filter.value());
            case NE -> cb.or(cb.notEqual(value, filter.value()), value.isNull());
            case IN -> value.in(filter.values());
            case GTE -> cb.greaterThanOrEqualTo(value, filter.value());
            case LTE -> cb.lessThanOrEqualTo(value, filter.value());
        };
    }

    private static Predicate fixedFieldPredicate(ItemFilter filter, Root<Item> root, CriteriaBuilder cb,
                                                 String currentUser) {
        Function<Root<Item>, Path<?>> path = FIXED_FIELDS.get(filter.field());
        if (path == null) {
            throw new BadRequestException("Unknown filter field '" + filter.field() + "'");
        }
        Path<?> attribute = path.apply(root);
        List<Object> values = filter.values().stream()
                .map(value -> typed(filter.field(), value, currentUser))
                .toList();
        return switch (filter.operator()) {
            case EQ -> cb.equal(attribute, values.getFirst());
            case NE -> cb.or(cb.notEqual(attribute, values.getFirst()), attribute.isNull());
            case IN -> attribute.in(values);
            case GTE -> atLeast(cb, attribute, values.getFirst());
            case LTE -> atMost(cb, attribute, values.getFirst());
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate atLeast(CriteriaBuilder cb, Path<?> attribute, Object value) {
        return cb.greaterThanOrEqualTo((Expression) attribute, (Comparable) value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate atMost(CriteriaBuilder cb, Path<?> attribute, Object value) {
        return cb.lessThanOrEqualTo((Expression) attribute, (Comparable) value);
    }

    private static Object typed(String field, String value, String currentUser) {
        return switch (field) {
            case "assignee", "reporter" -> CURRENT_USER.equals(value) ? currentUser : value;
            case "statusCategory" -> enumValue(StatusCategory.class, value, field);
            case "priority" -> enumValue(Priority.class, value, field);
            case "createdAt", "updatedAt" -> instant(value, field);
            default -> value;
        };
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("'" + value + "' is not a valid " + field);
        }
    }

    private static Instant instant(String value, String field) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("'" + value + "' is not an ISO instant for " + field);
        }
    }
}
