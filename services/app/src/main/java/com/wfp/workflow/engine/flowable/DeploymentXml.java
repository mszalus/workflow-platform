package com.wfp.workflow.engine.flowable;

import com.wfp.common.exception.BadRequestException;
import com.wfp.workflow.engine.Flow;
import com.wfp.workflow.engine.InvalidWorkflowException;
import com.wfp.workflow.engine.Node;
import com.wfp.workflow.engine.NodeType;
import com.wfp.workflow.engine.TrackerProfileValidator;
import com.wfp.workflow.engine.Violation;
import com.wfp.workflow.engine.WorkflowGraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class DeploymentXml {

    private final FlowableWorkflowParser parser = new FlowableWorkflowParser();
    private final TrackerProfileValidator validator = new TrackerProfileValidator();

    String prepare(String bpmnXml) {
        Document document = parse(bpmnXml);
        NodeList processes = document.getElementsByTagNameNS("*", "process");
        for (int i = 0; i < processes.getLength(); i++) {
            ((Element) processes.item(i)).setAttribute("isExecutable", "true");
        }
        if (usesStatusCategories(document)) {
            WorkflowGraph graph = parser.parse(bpmnXml);
            List<Violation> violations = validator.validate(graph);
            if (!violations.isEmpty()) {
                throw new InvalidWorkflowException(violations);
            }
            setTransitionConditions(document, transitionFlowIds(graph));
        }
        return serialize(document);
    }

    private static boolean usesStatusCategories(Document document) {
        NodeList elements = document.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < elements.getLength(); i++) {
            if (((Element) elements.item(i)).hasAttributeNS(FlowableWorkflowParser.TRACKER_NAMESPACE,
                    FlowableWorkflowParser.STATUS_CATEGORY)) {
                return true;
            }
        }
        return false;
    }

    private static void setTransitionConditions(Document document, Set<String> transitionFlowIds) {
        NodeList flows = document.getElementsByTagNameNS("*", "sequenceFlow");
        for (int i = 0; i < flows.getLength(); i++) {
            Element flow = (Element) flows.item(i);
            if (transitionFlowIds.contains(flow.getAttribute("id"))) {
                removeConditions(flow);
                Element condition = document.createElementNS(flow.getNamespaceURI(),
                        qualified(flow.getPrefix(), "conditionExpression"));
                condition.setTextContent("${transition == '" + flow.getAttribute("id") + "'}");
                flow.appendChild(condition);
            }
        }
    }

    private static Set<String> transitionFlowIds(WorkflowGraph graph) {
        return graph.statuses().stream()
                .filter(status -> status.type() == NodeType.USER_TASK)
                .flatMap(status -> graph.outgoing(status.id()).stream())
                .map(leaving -> graph.node(leaving.targetId()).orElseThrow())
                .filter(target -> target.type() == NodeType.EXCLUSIVE_GATEWAY)
                .map(Node::id)
                .flatMap(gatewayId -> graph.outgoing(gatewayId).stream())
                .map(Flow::id)
                .collect(Collectors.toSet());
    }

    private static void removeConditions(Element flow) {
        NodeList conditions = flow.getElementsByTagNameNS("*", "conditionExpression");
        while (conditions.getLength() > 0) {
            flow.removeChild(conditions.item(0));
        }
    }

    private static String qualified(String prefix, String localName) {
        return prefix == null ? localName : prefix + ":" + localName;
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new BadRequestException("The workflow is not readable BPMN XML");
        }
    }

    private static String serialize(Document document) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            StringWriter xml = new StringWriter();
            factory.newTransformer().transform(new DOMSource(document), new StreamResult(xml));
            return xml.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("Could not write the workflow XML", e);
        }
    }
}
