package com.wfp.workflow.engine.flowable;

import com.wfp.workflow.engine.Flow;
import com.wfp.workflow.engine.Node;
import com.wfp.workflow.engine.NodeType;
import com.wfp.workflow.engine.WorkflowGraph;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.util.io.StringStreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowableWorkflowParser {

    public static final String TRACKER_NAMESPACE = "http://wfp.com/schema/tracker";
    private static final String STATUS_CATEGORY = "statusCategory";
    private static final String MULTI_INSTANCE_USER_TASK = "multi-instance userTask";

    public WorkflowGraph parse(String bpmnXml) {
        Map<String, String> statusCategories = readStatusCategories(bpmnXml);
        BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(new StringStreamSource(bpmnXml), false, false);
        if (model.getProcesses().size() != 1) {
            throw new IllegalArgumentException(
                    "A tracker workflow needs exactly one process, found " + model.getProcesses().size());
        }
        Process process = model.getProcesses().getFirst();
        List<Node> nodes = new ArrayList<>();
        List<Flow> flows = new ArrayList<>();
        collect(process.getFlowElements(), null, statusCategories, nodes, flows);
        return new WorkflowGraph(process.getId(), nodes, flows);
    }

    private void collect(Collection<FlowElement> elements, String parentId, Map<String, String> statusCategories,
                         List<Node> nodes, List<Flow> flows) {
        for (FlowElement element : elements) {
            if (element instanceof SequenceFlow flow) {
                flows.add(new Flow(flow.getId(), flow.getName(), flow.getSourceRef(), flow.getTargetRef()));
                continue;
            }
            Node node = toNode(element, parentId, statusCategories.get(element.getId()));
            nodes.add(node);
            if (element instanceof SubProcess subProcess && node.type() != NodeType.UNSUPPORTED) {
                collect(subProcess.getFlowElements(), subProcess.getId(), statusCategories, nodes, flows);
            }
        }
    }

    private Node toNode(FlowElement element, String parentId, String statusCategory) {
        List<String> candidateGroups = element instanceof UserTask userTask
                ? List.copyOf(userTask.getCandidateGroups())
                : List.of();
        String attachedToId = element instanceof BoundaryEvent boundary ? boundary.getAttachedToRefId() : null;
        return new Node(element.getId(), element.getName(), nodeType(element), elementType(element), statusCategory,
                parentId, attachedToId, candidateGroups);
    }

    private NodeType nodeType(FlowElement element) {
        return switch (element) {
            case StartEvent start -> NodeType.START;
            case EndEvent end -> NodeType.END;
            case UserTask userTask -> NodeType.USER_TASK;
            case ServiceTask serviceTask -> NodeType.SERVICE_TASK;
            case ExclusiveGateway gateway -> NodeType.EXCLUSIVE_GATEWAY;
            case ParallelGateway gateway -> NodeType.PARALLEL_GATEWAY;
            case EventSubProcess eventSubProcess ->
                    startsOnInterruptingMessage(eventSubProcess) ? NodeType.EVENT_SUBPROCESS : NodeType.UNSUPPORTED;
            case SubProcess subProcess when subProcess.getClass() == SubProcess.class -> NodeType.SUBPROCESS;
            case BoundaryEvent boundary ->
                    isInterruptingTimer(boundary) ? NodeType.BOUNDARY_TIMER : NodeType.UNSUPPORTED;
            default -> NodeType.UNSUPPORTED;
        };
    }

    private boolean startsOnInterruptingMessage(EventSubProcess eventSubProcess) {
        return eventSubProcess.getFlowElements().stream()
                .filter(StartEvent.class::isInstance)
                .map(StartEvent.class::cast)
                .filter(StartEvent::isInterrupting)
                .flatMap(start -> start.getEventDefinitions().stream())
                .anyMatch(MessageEventDefinition.class::isInstance);
    }

    private boolean isInterruptingTimer(BoundaryEvent boundary) {
        return hasTimer(boundary) && boundary.isCancelActivity();
    }

    private boolean hasTimer(BoundaryEvent boundary) {
        return boundary.getEventDefinitions().stream().anyMatch(TimerEventDefinition.class::isInstance);
    }

    private String elementType(FlowElement element) {
        return switch (element) {
            case BoundaryEvent boundary when hasTimer(boundary) && !boundary.isCancelActivity() ->
                    "non-interrupting boundary timer";
            case EventSubProcess eventSubProcess -> "eventSubProcess without an interrupting message start";
            case UserTask userTask when userTask.getLoopCharacteristics() != null -> MULTI_INSTANCE_USER_TASK;
            default -> {
                String className = element.getClass().getSimpleName();
                yield Character.toLowerCase(className.charAt(0)) + className.substring(1);
            }
        };
    }

    private Map<String, String> readStatusCategories(String bpmnXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));
            NodeList elements = document.getElementsByTagNameNS("*", "*");
            Map<String, String> statusCategories = new HashMap<>();
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                if (element.hasAttributeNS(TRACKER_NAMESPACE, STATUS_CATEGORY)) {
                    statusCategories.put(element.getAttribute("id"),
                            element.getAttributeNS(TRACKER_NAMESPACE, STATUS_CATEGORY));
                }
            }
            return statusCategories;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("The workflow is not readable BPMN XML", e);
        }
    }
}
