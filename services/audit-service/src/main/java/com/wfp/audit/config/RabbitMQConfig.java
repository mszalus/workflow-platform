package com.wfp.audit.config;

import com.wfp.events.EventConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange wfpEventsExchange() {
        return new TopicExchange(EventConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue auditQueue() {
        return new Queue(EventConstants.AUDIT_QUEUE, true);
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange wfpEventsExchange) {
        return BindingBuilder.bind(auditQueue).to(wfpEventsExchange).with(EventConstants.ALL_EVENTS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
