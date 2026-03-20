package com.workflowplatform.notification.websocket;

import com.workflowplatform.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotificationSender {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a notification to a specific user's personal queue.
     * The STOMP destination will be: /user/{userId}/queue/notifications
     */
    public void sendToUser(String userId, Notification notification) {
        try {
            Map<String, Object> payload = Map.of(
                "id", notification.getId().toString(),
                "type", notification.getNotificationType().name(),
                "title", notification.getTitle(),
                "body", notification.getBody() != null ? notification.getBody() : "",
                "referenceId", notification.getReferenceId() != null ? notification.getReferenceId() : "",
                "referenceType", notification.getReferenceType() != null ? notification.getReferenceType() : "",
                "actionUrl", notification.getActionUrl() != null ? notification.getActionUrl() : "",
                "createdAt", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : ""
            );

            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);

            log.debug("Sent WebSocket notification to userId={} type={}",
                userId, notification.getNotificationType());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to userId={}", userId, e);
        }
    }

    /**
     * Broadcast a notification to all subscribers of a topic.
     *
     * @param topic   e.g. "/topic/tenant/{tenantId}/announcements"
     * @param payload message payload
     */
    public void broadcast(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
        } catch (Exception e) {
            log.error("Failed to broadcast to topic={}", topic, e);
        }
    }
}
