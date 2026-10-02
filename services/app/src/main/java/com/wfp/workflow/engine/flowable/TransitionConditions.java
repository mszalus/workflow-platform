package com.wfp.workflow.engine.flowable;

import com.wfp.workflow.engine.Flow;
import com.wfp.workflow.engine.Node;
import com.wfp.workflow.engine.NodeType;
import com.wfp.workflow.engine.WorkflowGraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Collectors;

class TransitionConditions {

    private final FlowableWorkflowParser parser = new FlowableWorkflowParser();

    String addTo(String bpmnXml) {
        if (!bpmnXml.contains(FlowableWorkflowParser.TRACKER_NAMESPACE)) {
            return bpmnXml;
        }
        Set<String> transitionFlowIds = transitionFlowIds(parser.parse(bpmnXml));
        try {
            Document document = parse(bpmnXml);
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
            return serialize(document);
        } catch (Exception e) {
            throw new IllegalArgumentException("The workflow is not readable BPMN XML", e);
        }
    }

    private Set<String> transitionFlowIds(WorkflowGraph graph) {
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

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        StringWriter xml = new StringWriter();
        factory.newTransformer().transform(new DOMSource(document), new StreamResult(xml));
        return xml.toString();
    }
}
