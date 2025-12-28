package org.dataingest.rfc.server.idoc;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        // Parse XML with UTF-8 encoding
        try (FileInputStream fis = new FileInputStream(xmlFilePath);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            Document doc = builder.parse(new org.xml.sax.InputSource(isr));

            String json = parseIdocXml(doc.getDocumentElement());

            // Write JSON with UTF-8 encoding
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(jsonFilePath), StandardCharsets.UTF_8)) {
                writer.write(json);
                writer.flush();
            }
        }
    }

    /**
     * Parse IDOC XML and return clean JSON structure
     * Groups segments by type with fields array for multiple occurrences
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
        json.append("\t\"control\": {\n");
        if (controlSegment != null) {
            json.append(buildFieldsObject(controlSegment, true));
        }
        json.append("\t}");

        // Group segments by segment name
        Map<String, List<Element>> groupedSegments = new LinkedHashMap<>();
        for (Element segment : dataSegments) {
            String segmentName = segment.getTagName();
            if (!groupedSegments.containsKey(segmentName)) {
                groupedSegments.put(segmentName, new ArrayList<>());
            }
            groupedSegments.get(segmentName).add(segment);
        }

        // Build segments grouped by type
        for (Map.Entry<String, List<Element>> entry : groupedSegments.entrySet()) {
            json.append(",\n");
            json.append(buildGroupedSegment(entry.getKey(), entry.getValue()));
        }

        json.append("\n}\n");

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
            json.append("\t\t\"").append(entry.getKey()).append("\": \"");
            json.append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }

        if (!first) {
            json.append("\n\t");
        }

        return json.toString();
    }

    /**
     * Build a grouped segment with all occurrences in fields array
     */
    private static String buildGroupedSegment(String segmentName, List<Element> segments) {
        StringBuilder json = new StringBuilder();

        if (segments.isEmpty()) {
            return "";
        }

        // Get metadata from first occurrence (should be same for all)
        Element firstSegment = segments.get(0);

        json.append("\t\"").append(segmentName).append("\": {\n");
        json.append("\t\t\"segment_number\": \"").append(getSegmentAttribute(firstSegment, "SEGMENT", "1")).append("\",\n");
        json.append("\t\t\"parent_segment\": \"").append(getSegmentAttribute(firstSegment, "PARENT", "000000")).append("\",\n");
        json.append("\t\t\"hierarchy_level\": \"").append(getSegmentAttribute(firstSegment, "HLEVEL", "00")).append("\",\n");
        json.append("\t\t\"fields\": [\n");

        // Add all occurrences as separate objects in fields array
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                json.append(",\n");
            }
            json.append("\t\t\t{\n");

            Element segment = segments.get(i);
            Map<String, String> fields = extractFields(segment);
            List<Element> nestedSegments = extractNestedSegments(segment);

            // Add fields
            boolean first = true;
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (!first) {
                    json.append(",\n");
                }
                json.append("\t\t\t\t\"").append(entry.getKey()).append("\": \"");
                json.append(escapeJson(entry.getValue())).append("\"");
                first = false;
            }

            // Add nested segments
            if (!nestedSegments.isEmpty()) {
                for (Element nestedSegment : nestedSegments) {
                    if (!first) {
                        json.append(",\n");
                    }
                    json.append(buildNestedSegment(nestedSegment, 4));  // 4 tabs indentation
                    first = false;
                }
            }

            json.append("\n\t\t\t}");
        }

        json.append("\n\t\t]\n");
        json.append("\t}");

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

                // Check if this is a nested segment (has SEGMENT attribute or contains child elements)
                if (isNestedSegment(child)) {
                    continue;  // Skip nested segments, they'll be processed separately
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
     * Check if an element is a nested segment (vs a simple field)
     */
    private static boolean isNestedSegment(Element element) {
        // Has SEGMENT attribute
        if (element.hasAttribute("SEGMENT")) {
            return true;
        }

        // Check if it has child elements (not just text)
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;  // Has child elements, it's a segment
            }
        }

        return false;  // Just text content, it's a field
    }

    /**
     * Extract nested segments from a parent segment
     */
    private static List<Element> extractNestedSegments(Element segment) {
        List<Element> nestedSegments = new ArrayList<>();

        NodeList children = segment.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                if (isNestedSegment(child)) {
                    nestedSegments.add(child);
                }
            }
        }

        return nestedSegments;
    }

    /**
     * Build a nested segment with proper indentation
     */
    private static String buildNestedSegment(Element segment, int indentLevel) {
        StringBuilder json = new StringBuilder();
        String indent = "\t".repeat(indentLevel);
        String segmentName = segment.getTagName();

        json.append(indent).append("\"").append(segmentName).append("\": {\n");

        // Extract fields
        Map<String, String> fields = extractFields(segment);
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            json.append(indent).append("\t\"").append(entry.getKey()).append("\": \"");
            json.append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }

        // Recursively handle nested segments
        List<Element> nestedSegments = extractNestedSegments(segment);
        if (!nestedSegments.isEmpty()) {
            for (Element nested : nestedSegments) {
                if (!first) {
                    json.append(",\n");
                }
                json.append(buildNestedSegment(nested, indentLevel + 1));
                first = false;
            }
        }

        json.append("\n").append(indent).append("}");

        return json.toString();
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
