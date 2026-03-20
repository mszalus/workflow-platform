package com.workflowplatform.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final RestTemplate restTemplate;

    @Value("${notification.from-address:noreply@workflow.local}")
    private String fromAddress;

    @Value("${identity-proxy.base-url:http://localhost:8084}")
    private String identityProxyBaseUrl;

    @Async
    public void sendTaskAssignedEmail(String recipientId,
                                      String taskName,
                                      String taskId,
                                      String processInstanceId,
                                      String actionUrl) {
        log.info("Sending task-assigned email to userId={} taskId={}", recipientId, taskId);

        String recipientEmail = resolveEmailFromIdentityService(recipientId);
        if (recipientEmail == null) {
            log.warn("Could not resolve email for userId={}, skipping task-assigned email for taskId={}", recipientId, taskId);
            return;
        }

        Context context = new Context();
        context.setVariable("taskName", taskName);
        context.setVariable("taskId", taskId);
        context.setVariable("processInstanceId", processInstanceId);
        context.setVariable("actionUrl", actionUrl);

        sendHtmlEmail(
            recipientEmail,
            "Task Assigned: " + taskName,
            "emails/task-assigned",
            context,
            recipientId
        );
    }

    @Async
    public void sendProcessCompletedEmail(String recipientId,
                                          String processInstanceId,
                                          String processDefinitionKey) {
        log.info("Sending process-completed email to userId={} processInstanceId={}",
            recipientId, processInstanceId);

        String recipientEmail = resolveEmailFromIdentityService(recipientId);
        if (recipientEmail == null) {
            log.warn("Could not resolve email for userId={}, skipping process-completed email for processInstanceId={}", recipientId, processInstanceId);
            return;
        }

        Context context = new Context();
        context.setVariable("processInstanceId", processInstanceId);
        context.setVariable("processDefinitionKey", processDefinitionKey);

        sendHtmlEmail(
            recipientEmail,
            "Process Completed: " + processDefinitionKey,
            "emails/process-completed",
            context,
            recipientId
        );
    }

    @Async
    public void sendTaskDueSoonEmail(String recipientEmail,
                                     String recipientId,
                                     String taskName,
                                     String taskId,
                                     String dueDate,
                                     String actionUrl) {
        log.info("Sending task-due-soon email to userId={} taskId={}", recipientId, taskId);

        Context context = new Context();
        context.setVariable("taskName", taskName);
        context.setVariable("taskId", taskId);
        context.setVariable("dueDate", dueDate);
        context.setVariable("actionUrl", actionUrl);

        sendHtmlEmail(
            recipientEmail,
            "Task Due Soon: " + taskName,
            "emails/task-due-soon",
            context,
            recipientId
        );
    }

    @SuppressWarnings("unchecked")
    private String resolveEmailFromIdentityService(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            String url = identityProxyBaseUrl + "/api/v1/users/" + userId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("email") instanceof String email && !email.isBlank()) {
                return email;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to resolve email for userId={} from identity-proxy-service: {}", userId, e.getMessage());
            return null;
        }
    }

    private void sendHtmlEmail(String recipientEmail,
                                String subject,
                                String templateName,
                                Context context,
                                String recipientId) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("No email address available for userId={}, skipping email send for subject='{}'",
                recipientId, subject);
            return;
        }

        try {
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email sent successfully to={} subject='{}'", recipientEmail, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to={} subject='{}'", recipientEmail, subject, e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to={}", recipientEmail, e);
        }
    }
}
