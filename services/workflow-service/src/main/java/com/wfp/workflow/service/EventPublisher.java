package com.wfp.workflow.service;

import com.wfp.events.BaseEvent;
import com.wfp.events.EventConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(@Nullable RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String routingKey, BaseEvent event) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate not available, skipping event [{}]", routingKey);
            return;
        }
        log.info("Publishing event [{}] for tenant [{}]", routingKey, event.getTenantId());
        rabbitTemplate.convertAndSend(EventConstants.EXCHANGE, routingKey, event);
    }
}
