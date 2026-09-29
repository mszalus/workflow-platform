package com.wfp.audit.config;

import com.wfp.events.TaskCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    @Test
    void convertsWorkflowEventsNamedInTheTypeIdHeader() {
        MessageConverter converter = new RabbitMQConfig().jackson2JsonMessageConverter();
        Message message = converter.toMessage(TaskCreatedEvent.builder().taskId("task-1").build(), new MessageProperties());

        assertThat(converter.fromMessage(message))
                .isInstanceOfSatisfying(TaskCreatedEvent.class, event -> assertThat(event.getTaskId()).isEqualTo("task-1"));
    }
}
