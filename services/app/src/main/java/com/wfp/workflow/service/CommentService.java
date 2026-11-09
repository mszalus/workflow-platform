package com.wfp.workflow.service;

import com.wfp.common.exception.NotFoundException;
import com.wfp.workflow.dto.CommentDto;
import com.wfp.workflow.entity.Comment;
import com.wfp.workflow.entity.Item;
import com.wfp.workflow.entity.NotificationType;
import com.wfp.workflow.repository.CommentRepository;
import com.wfp.workflow.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public CommentDto addToItem(String key, String content, String actor) {
        Item item = requireItem(key);
        Comment comment = commentRepository.save(Comment.builder()
                .itemId(item.getId())
                .userId(actor)
                .content(content)
                .tenantId(item.getTenantId())
                .build());
        auditService.record("item.commented", "ITEM", item.getKey(), actor,
                Map.of("commentId", comment.getId().toString()));
        notifyReporterAndAssignee(item, actor);
        return toDto(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> listForItem(String key) {
        Item item = requireItem(key);
        return commentRepository.findByItemIdAndTenantIdOrderByCreatedAtAsc(item.getId(), item.getTenantId())
                .stream().map(this::toDto).toList();
    }

    private void notifyReporterAndAssignee(Item item, String actor) {
        Stream.of(item.getReporter(), item.getAssignee())
                .filter(Objects::nonNull)
                .filter(recipient -> !recipient.equals(actor))
                .distinct()
                .forEach(recipient -> notificationService.createNotification(recipient, item.getTenantId(),
                        actor + " commented on " + item.getKey(), item.getTitle(),
                        NotificationType.ITEM_COMMENTED, item.getKey(), "ITEM"));
    }

    private Item requireItem(String key) {
        return itemRepository.findByKey(key).orElseThrow(() -> new NotFoundException("Item", key));
    }

    private CommentDto toDto(Comment c) {
        return CommentDto.builder()
                .id(c.getId() != null ? c.getId().toString() : null)
                .userId(c.getUserId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
