package it.unive.jlisa.witness;

import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class WitnessWriter {

	private Document doc;
	private Element root;
	private Element graph;

	public WitnessWriter()
			throws Exception {
		initDocument();
	}

	private void initDocument() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc = builder.newDocument();

		// Root element
		root = doc.createElement("graphml");
		root.setAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns");
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		doc.appendChild(root);

		// Add keys
		addKey("originFileName", "string", "edge", "originfile", "<command-line>");
		addKey("invariant", "string", "node", "invariant", null);
		addKey("invariant.scope", "string", "node", "invariant.scope", null);
		addKey("isViolationNode", "boolean", "node", "violation", "false");
		addKey("isEntryNode", "boolean", "node", "entry", "false");
		addKey("isSinkNode", "boolean", "node", "sink", "false");
		addKey("enterLoopHead", "boolean", "edge", "enterLoopHead", "false");
		addKey("cyclehead", "boolean", "edge", "cyclehead", "false");
		addKey("threadId", "int", "edge", "threadId", "0");
		addKey("createThread", "int", "edge", "createThread", "0");
		addKey("sourcecodeLanguage", "string", "graph", "sourcecodelang", null);
		addKey("programFile", "string", "graph", "programfile", null);
		addKey("programHash", "string", "graph", "programhash", null);
		addKey("specification", "string", "graph", "specification", null);
		addKey("architecture", "string", "graph", "architecture", null);
		addKey("producer", "string", "graph", "producer", null);
		addKey("startline", "int", "edge", "startline", null);
		addKey("control", "string", "edge", "control", null);
		addKey("assumption", "string", "edge", "assumption", null);
		addKey("assumption.resultfunction", "string", "edge", "assumption.resultfunction", null);
		addKey("assumption.scope", "string", "edge", "assumption.scope", null);
		addKey("enterFunction", "string", "edge", "enterFunction", null);
		addKey("returnFromFunction", "string", "edge", "returnFrom", null);
		addKey("witness-type", "string", "graph", "witness-type", null);

		// Graph element
		graph = doc.createElement("graph");
		graph.setAttribute("edgedefault", "directed");
		root.appendChild(graph);

		// Default graph-level data
		addData(graph, "witness-type", "violation_witness");
		addData(graph, "producer", "JLiSA");
	}

	private void addKey(
			String name,
			String type,
			String forType,
			String id,
			String defaultValue) {
		Element key = doc.createElement("key");
		key.setAttribute("attr.name", name);
		key.setAttribute("attr.type", type);
		key.setAttribute("for", forType);
		key.setAttribute("id", id);
		if (defaultValue != null) {
			Element def = doc.createElement("default");
			def.setTextContent(defaultValue);
			key.appendChild(def);
		}
		root.appendChild(key);
	}

	private void addData(
			Element parent,
			String key,
			String value) {
		Element data = doc.createElement("data");
		data.setAttribute("key", key);
		data.setTextContent(value);
		parent.appendChild(data);
	}

	public void addNode(
			String id,
			Map<String, String> attributes) {
		Element node = doc.createElement("node");
		node.setAttribute("id", id);
		if (attributes != null) {
			for (var entry : attributes.entrySet()) {
				addData(node, entry.getKey(), entry.getValue());
			}
		}
		graph.appendChild(node);
	}

	public void addEdge(
			String source,
			String target,
			Map<String, String> attributes) {
		Element edge = doc.createElement("edge");
		edge.setAttribute("source", source);
		edge.setAttribute("target", target);
		if (attributes != null) {
			for (var entry : attributes.entrySet()) {
				addData(edge, entry.getKey(), entry.getValue());
			}
		}
		graph.appendChild(edge);
	}

	public void writeToFile(
			String fileName)
			throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		transformer.transform(new DOMSource(doc), new StreamResult(new File(fileName)));
	}
}
