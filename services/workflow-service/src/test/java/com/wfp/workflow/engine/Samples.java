package com.wfp.workflow.engine;

import com.wfp.workflow.engine.flowable.FlowableWorkflowParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class Samples {

    private Samples() {}

    public static WorkflowGraph graph(String sample) {
        return new FlowableWorkflowParser().parse(xml(sample));
    }

    public static String xml(String sample) {
        try (InputStream in = Samples.class.getResourceAsStream("/workflows/" + sample + ".bpmn20.xml")) {
            if (in == null) {
                throw new IllegalArgumentException("No sample workflow " + sample);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
