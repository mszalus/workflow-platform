package com.workflowplatform.engine.config;

import com.workflowplatform.engine.event.WorkflowFlowableEventListener;
import lombok.RequiredArgsConstructor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class FlowableConfig {

    private final WorkflowFlowableEventListener workflowFlowableEventListener;

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> flowableEngineConfigurer() {
        return config -> {
            config.setEventListeners(List.of(workflowFlowableEventListener));
        };
    }
}
