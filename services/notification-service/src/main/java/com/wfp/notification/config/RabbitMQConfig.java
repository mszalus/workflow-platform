package com.wfp.notification.config;

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
    public Queue notificationQueue() {
        return new Queue(EventConstants.NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding taskCreatedBinding(Queue notificationQueue, TopicExchange wfpEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(wfpEventsExchange).with("task.*");
    }

    @Bean
    public Binding processCompletedBinding(Queue notificationQueue, TopicExchange wfpEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(wfpEventsExchange).with("process.completed");
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
