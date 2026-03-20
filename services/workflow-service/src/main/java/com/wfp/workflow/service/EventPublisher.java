package com.wfp.workflow.service;

import com.wfp.events.BaseEvent;
import com.wfp.events.EventConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String routingKey, BaseEvent event) {
        log.info("Publishing event [{}] for tenant [{}]", routingKey, event.getTenantId());
        rabbitTemplate.convertAndSend(EventConstants.EXCHANGE, routingKey, event);
    }
}
