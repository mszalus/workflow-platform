package com.wfp.workflow.service;

import com.wfp.workflow.entity.Notification;
import com.wfp.workflow.entity.NotificationType;
import com.wfp.workflow.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationService notificationService = new NotificationService(notificationRepository);

    @Test
    void createsANotificationInTheGivenTenant() {
        notificationService.createNotification("user-a", "tenant-a", "PROJ-1 assigned to you", "Fix login",
                NotificationType.ITEM_ASSIGNED, "PROJ-1", "ITEM");

        assertThat(savedNotification())
                .extracting(Notification::getUserId, Notification::getTenantId, Notification::getType,
                        Notification::getReferenceId)
                .containsExactly("user-a", "tenant-a", NotificationType.ITEM_ASSIGNED, "PROJ-1");
    }

    @Test
    void truncatesTitlesToTheColumnLength() {
        notificationService.createNotification("user-a", "tenant-a", "x".repeat(300), "Fix login",
                NotificationType.INFO, "PROJ-1", "ITEM");

        assertThat(savedNotification().getTitle()).hasSize(255);
    }

    private Notification savedNotification() {
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        return saved.getValue();
    }
}
