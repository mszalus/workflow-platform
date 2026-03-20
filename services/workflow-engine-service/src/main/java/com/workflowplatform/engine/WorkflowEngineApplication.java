package com.workflowplatform.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class WorkflowEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowEngineApplication.class, args);
    }
}
