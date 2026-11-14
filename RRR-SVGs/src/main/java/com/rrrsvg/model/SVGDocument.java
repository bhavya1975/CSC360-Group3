package com.rrrsvg.model;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed SVG document, encapsulating the root &lt;svg&gt; element,
 * the complete hierarchical tree, and document-level query utilities.
 */
public class SVGDocument {

    private final SVGRoot root;
    private final String source;

    /**
     * Constructs a new SVGDocument.
     *
     * @param root   The root &lt;svg&gt; element.
     * @param source Identifier or file path of the source.
     */
    public SVGDocument(SVGRoot root, String source) {
        if (root == null) {
            throw new IllegalArgumentException("Root element cannot be null");
        }
        this.root = root;
        this.source = source != null ? source : "unknown";
    }

    /**
     * Returns the root &lt;svg&gt; element of this document.
     *
     * @return Root element.
     */
    public SVGRoot getRoot() {
        return root;
    }

    /**
     * Returns the source file or resource name.
     *
     * @return Source name or path.
     */
    public String getSource() {
        return source;
    }

    /**
     * Collects all elements in the SVG document tree in pre-order traversal.
     *
     * @return List of all elements including root.
     */
    public List<SVGElement> getAllElements() {
        List<SVGElement> all = new ArrayList<>();
        root.collectAllElements(all);
        return all;
    }

    /**
     * Finds all elements in the document matching the given tag name.
     *
     * @param tagName Tag name (case-insensitive, e.g. "rect", "circle").
     * @return List of matching elements.
     */
    public List<SVGElement> getElementsByTagName(String tagName) {
        List<SVGElement> matches = new ArrayList<>();
        if (tagName != null && (tagName.equalsIgnoreCase(SVGRoot.TAG_NAME) || "*".equals(tagName))) {
            matches.add(root);
        }
        matches.addAll(root.getElementsByTagName(tagName));
        return matches;
    }

    /**
     * Finds an element anywhere in the document by its 'id' attribute.
     *
     * @param id Element identifier.
     * @return Matching SVGElement, or null if not found.
     */
    public SVGElement getElementById(String id) {
        return root.getElementById(id);
    }

    /**
     * Returns the total number of elements in the document (including root).
     *
     * @return Total element count.
     */
    public int getTotalElementCount() {
        return getAllElements().size();
    }

    /**
     * Returns a summary map counting each element type found in the document.
     *
     * @return Map of tag name to occurrence count.
     */
    public Map<String, Integer> getElementTypeCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SVGElement element : getAllElements()) {
            counts.put(element.getTagName(), counts.getOrDefault(element.getTagName(), 0) + 1);
        }
        return counts;
    }

    /**
     * Formats the entire document structure and element attributes into a readable tree.
     *
     * @return Formatted hierarchical tree string.
     */
    public String toTreeString() {
        return root.toTreeString();
    }

    /**
     * Prints the hierarchical document tree to {@link System#out}.
     */
    public void printTree() {
        printTree(System.out);
    }

    /**
     * Prints the hierarchical document tree to the specified PrintStream.
     *
     * @param out Target output stream.
     */
    public void printTree(PrintStream out) {
        out.println("================================================================================");
        out.println("SVG Document: " + source);
        out.println("Dimensions  : " + root.getWidth() + " x " + root.getHeight() +
                (root.getViewBox() != null ? " (viewBox: " + root.getViewBox() + ")" : ""));
        out.println("Total Nodes : " + getTotalElementCount());
        out.println("================================================================================");
        out.println("HIERARCHICAL STRUCTURE & ATTRIBUTES:");
        out.println("--------------------------------------------------------------------------------");
        out.print(root.toTreeString());
        out.println("--------------------------------------------------------------------------------");
        out.println("ELEMENT BREAKDOWN:");
        for (Map.Entry<String, Integer> entry : getElementTypeCounts().entrySet()) {
            out.printf("  - %-12s: %d%n", entry.getKey(), entry.getValue());
        }
        out.println("================================================================================");
    }

    @Override
    public String toString() {
        return "SVGDocument[source=" + source + ", elements=" + getTotalElementCount() +
                ", width=" + root.getWidth() + ", height=" + root.getHeight() + "]";
    }
}

