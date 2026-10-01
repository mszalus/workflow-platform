package com.wfp.workflow.service;

import com.wfp.workflow.event.EventConstants;
import com.wfp.workflow.event.TaskCompletedEvent;
import com.wfp.workflow.event.TaskCreatedEvent;
import com.wfp.workflow.entity.Notification;
import com.wfp.workflow.entity.NotificationType;
import com.wfp.workflow.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationService notificationService = new NotificationService(notificationRepository);

    @Test
    void notifiesTheAssigneeOfANewTaskInTheEventTenant() {
        TaskCreatedEvent event = TaskCreatedEvent.builder().taskId("task-1").taskName("Review").assignee("user-a").build();
        event.initDefaults(EventConstants.TASK_CREATED, "tenant-a", "user-a");

        notificationService.notify(event);

        Notification saved = savedNotification();
        assertThat(saved.getUserId()).isEqualTo("user-a");
        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
    }

    @Test
    void notifiesTheUserWhoCompletedATask() {
        TaskCompletedEvent event = TaskCompletedEvent.builder().taskId("task-1").taskName("Review").completedBy("user-b").build();
        event.initDefaults(EventConstants.TASK_COMPLETED, "tenant-a", "user-b");

        notificationService.notify(event);

        assertThat(savedNotification())
                .extracting(Notification::getUserId, Notification::getType)
                .containsExactly("user-b", NotificationType.TASK_COMPLETED);
    }

    @Test
    void truncatesTitlesToTheColumnLength() {
        TaskCreatedEvent event = TaskCreatedEvent.builder().taskId("task-1").taskName("x".repeat(255)).assignee("user-a").build();
        event.initDefaults(EventConstants.TASK_CREATED, "tenant-a", "user-a");

        notificationService.notify(event);

        assertThat(savedNotification().getTitle()).hasSize(255).startsWith("New Task: ");
    }

    @Test
    void skipsNewTasksWithoutAnAssignee() {
        TaskCreatedEvent event = TaskCreatedEvent.builder().taskId("task-1").taskName("Review").build();
        event.initDefaults(EventConstants.TASK_CREATED, "tenant-a", null);

        notificationService.notify(event);

        verify(notificationRepository, never()).save(any());
    }

    private Notification savedNotification() {
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        return saved.getValue();
    }
}
