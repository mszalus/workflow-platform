package com.wfp.workflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.events.EventConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(ConnectionFactory.class)
public class RabbitMQConfig {

    @Bean
    public TopicExchange wfpEventsExchange() {
        return new TopicExchange(EventConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(EventConstants.NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue auditQueue() {
        return new Queue(EventConstants.AUDIT_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange wfpEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(wfpEventsExchange)
                .with("task.*");
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange wfpEventsExchange) {
        return BindingBuilder.bind(auditQueue)
                .to(wfpEventsExchange)
                .with(EventConstants.ALL_EVENTS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EventConstants.EXCHANGE);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
