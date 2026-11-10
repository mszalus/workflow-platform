package com.wfp.workflow.engine.flowable;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BuiltInWorkflow implements ApplicationRunner {

    public static final String KEY = "default";
    static final String NAME = "Default";

    private final RepositoryService repositoryService;
    private final DeploymentXml deploymentXml = new DeploymentXml();

    @Override
    public void run(ApplicationArguments args) {
        repositoryService.createDeployment()
                .name(NAME)
                .addString(KEY + ".bpmn20.xml", deploymentXml.prepare(bundledXml()))
                .enableDuplicateFiltering()
                .deploy();
    }

    public static boolean isBuiltIn(ProcessDefinition definition) {
        return definition.getTenantId() == null || definition.getTenantId().isEmpty();
    }

    private static String bundledXml() {
        try {
            return new ClassPathResource("workflows/" + KEY + ".bpmn20.xml")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
