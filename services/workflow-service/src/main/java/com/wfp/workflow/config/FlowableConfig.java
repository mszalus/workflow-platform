package com.wfp.workflow.config;

import com.wfp.workflow.listener.FlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class FlowableConfig {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> engineConfigurer(
            FlowableEventListener listener) {
        return configuration -> {
            configuration.setDatabaseSchemaUpdate("true");
            configuration.setAsyncExecutorActivate(true);
            configuration.setTypedEventListeners(java.util.Map.of(
                    FlowableEngineEventType.TASK_CREATED.name() + ","
                    + FlowableEngineEventType.TASK_ASSIGNED.name() + ","
                    + FlowableEngineEventType.PROCESS_COMPLETED.name(),
                    Collections.singletonList(listener)
            ));
        };
    }
}
