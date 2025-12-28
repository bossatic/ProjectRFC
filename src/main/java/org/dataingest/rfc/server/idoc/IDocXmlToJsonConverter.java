package org.dataingest.rfc.server.idoc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Converts SAP IDoc XML to clean, readable JSON format
 * Matches the Python converter output structure
 */
public class IDocXmlToJsonConverter {

    /**
     * Convert IDoc XML file to JSON
     */
    public static void convertXmlToJson(String xmlFilePath, String jsonFilePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFilePath);

        String json = parseIdocXml(doc.getDocumentElement());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFilePath))) {
            writer.write(json);
            writer.flush();
        }
    }

    /**
     * Parse IDOC XML and return clean JSON structure
     */
    private static String parseIdocXml(Element root) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Find the IDOC element
        Element idocElement = root;
        if (!"IDOC".equals(root.getTagName())) {
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                Node node = root.getChildNodes().item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && "IDOC".equals(node.getNodeName())) {
                    idocElement = (Element) node;
                    break;
                }
            }
        }

        // Extract control segment and data segments
        Element controlSegment = null;
        List<Element> dataSegments = new ArrayList<>();

        NodeList children = idocElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if ("EDI_DC40".equals(element.getTagName())) {
                    controlSegment = element;
                } else {
                    dataSegments.add(element);
                }
            }
        }

        // Build control section
        json.append("  \"control\": {\n");
        if (controlSegment != null) {
            json.append(buildFieldsObject(controlSegment, true));
        }
        json.append("  },\n");

        // Build segments section
        json.append("  \"segments\": [\n");
        for (int i = 0; i < dataSegments.size(); i++) {
            if (i > 0) {
                json.append(",\n");
            }
            json.append(buildSegmentObject(dataSegments.get(i)));
        }
        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }

    /**
     * Build a control segment object
     */
    private static String buildFieldsObject(Element segment, boolean isControl) {
        StringBuilder json = new StringBuilder();
        Map<String, String> fields = extractFields(segment);

        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            json.append("    \"").append(entry.getKey()).append("\": \"");
            json.append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }

        if (!first) {
            json.append("\n  ");
        }

        return json.toString();
    }

    /**
     * Build a data segment object with metadata
     */
    private static String buildSegmentObject(Element segment) {
        StringBuilder json = new StringBuilder();
        json.append("    {\n");

        // Add segment metadata
        json.append("      \"segment_name\": \"").append(segment.getTagName()).append("\",\n");
        json.append("      \"segment_number\": \"").append(getSegmentAttribute(segment, "SEGMENT", "000000")).append("\",\n");
        json.append("      \"parent_segment\": \"").append(getSegmentAttribute(segment, "PARENT", "000000")).append("\",\n");
        json.append("      \"hierarchy_level\": \"").append(getSegmentAttribute(segment, "HLEVEL", "00")).append("\",\n");

        // Add fields
        json.append("      \"fields\": {\n");

        Map<String, String> fields = extractFields(segment);
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            json.append("        \"").append(entry.getKey()).append("\": \"");
            json.append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }

        json.append("\n      }\n");
        json.append("    }");

        return json.toString();
    }

    /**
     * Extract all fields from a segment element
     * Only extracts leaf fields (simple fields with text values, not nested segments)
     */
    private static Map<String, String> extractFields(Element segment) {
        Map<String, String> fields = new LinkedHashMap<>();

        NodeList children = segment.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                String fieldName = child.getTagName();

                // Skip if this looks like a nested segment (starts with E1 or matches segment patterns)
                if (fieldName.startsWith("E1") || fieldName.startsWith("E2") || fieldName.startsWith("Z")) {
                    continue;
                }

                // Get only direct text content (leaf value), not nested content
                String fieldValue = getDirectTextContent(child).trim();

                // Skip only empty fields - preserve all data including line breaks
                if (!fieldValue.isEmpty()) {
                    fields.put(fieldName, fieldValue);
                }
            }
        }

        return fields;
    }

    /**
     * Get direct text content of an element (not including nested elements)
     */
    private static String getDirectTextContent(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                text.append(node.getNodeValue());
            }
        }

        return text.toString();
    }

    /**
     * Get segment attribute value
     */
    private static String getSegmentAttribute(Element segment, String attrName, String defaultValue) {
        String value = segment.getAttribute(attrName);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Escape special JSON characters
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
