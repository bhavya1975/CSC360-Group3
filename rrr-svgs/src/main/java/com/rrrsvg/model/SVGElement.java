package com.rrrsvg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base abstract class representing an element in an SVG document tree.
 * <p>
 * Encapsulates tag name, attributes, parent-child relationships,
 * and convenient attribute accessors and hierarchical traversal methods.
 * </p>
 */
public abstract class SVGElement {

    private static final Pattern NUMERIC_CLEANER = Pattern.compile("^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)");

    protected final String tagName;
    protected final Map<String, String> attributes = new LinkedHashMap<>();
    protected final List<SVGElement> children = new ArrayList<>();
    protected SVGElement parent;

    /**
     * Constructs an SVGElement with the specified tag name.
     *
     * @param tagName Element tag name (e.g. "rect", "circle", "svg").
     */
    public SVGElement(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        this.tagName = tagName.trim().toLowerCase();
    }

    /**
     * Returns the tag name of this SVG element (in lowercase).
     *
     * @return Element tag name.
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Returns the 'id' attribute of this element if present.
     *
     * @return Element ID, or null if not defined.
     */
    public String getId() {
        return attributes.get("id");
    }

    /**
     * Sets or updates an attribute on this element.
     *
     * @param name  Attribute name.
     * @param value Attribute value.
     */
    public void setAttribute(String name, String value) {
        if (name != null) {
            attributes.put(name.trim(), value != null ? value.trim() : "");
        }
    }

    /**
     * Retrieves an attribute value by name.
     *
     * @param name Attribute name (case-sensitive as per XML attributes,
     *             or falls back to lowercase check).
     * @return Attribute value, or null if not found.
     */
    public String getAttribute(String name) {
        if (name == null) {
            return null;
        }
        String val = attributes.get(name);
        if (val != null) {
            return val;
        }
        // Fallback for case-insensitive lookup
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Retrieves an attribute value with a fallback default.
     *
     * @param name         Attribute name.
     * @param defaultValue Default value to return if attribute is absent.
     * @return Attribute value or defaultValue.
     */
    public String getAttribute(String name, String defaultValue) {
        String val = getAttribute(name);
        return val != null ? val : defaultValue;
    }

    /**
     * Checks if this element has the specified attribute.
     *
     * @param name Attribute name.
     * @return True if the attribute is present.
     */
    public boolean hasAttribute(String name) {
        return getAttribute(name) != null;
    }

    /**
     * Returns an unmodifiable map of all attributes on this element.
     *
     * @return Map of attribute names to attribute values.
     */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Parses an attribute value as a floating-point number, stripping any CSS/SVG
     * unit identifiers such as 'px', 'pt', 'mm', 'cm', etc.
     *
     * @param name         Attribute name.
     * @param defaultValue Default value if absent or unparseable.
     * @return Parsed double value.
     */
    public double getDoubleAttribute(String name, double defaultValue) {
        String raw = getAttribute(name);
        if (raw == null || raw.isEmpty()) {
            return defaultValue;
        }
        try {
            Matcher m = NUMERIC_CLEANER.matcher(raw.trim());
            if (m.find()) {
                return Double.parseDouble(m.group());
            }
            return defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Adds a child SVGElement to this element's hierarchy.
     *
     * @param child Child element to add.
     */
    public void addChild(SVGElement child) {
        if (child != null) {
            child.setParent(this);
            children.add(child);
        }
    }

    /**
     * Returns an unmodifiable list of this element's direct children.
     *
     * @return List of child elements.
     */
    public List<SVGElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Checks if this element has any child elements.
     *
     * @return True if children list is non-empty.
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Returns the parent element in the SVG hierarchy.
     *
     * @return Parent element, or null if this is the root.
     */
    public SVGElement getParent() {
        return parent;
    }

    /**
     * Sets the parent element in the SVG hierarchy.
     *
     * @param parent Parent element.
     */
    public void setParent(SVGElement parent) {
        this.parent = parent;
    }

    /**
     * Recursively retrieves all descendant elements matching a given tag name.
     *
     * @param targetTagName Tag name to match (case-insensitive, e.g. "rect", "circle").
     * @return List of matching elements.
     */
    public List<SVGElement> getElementsByTagName(String targetTagName) {
        List<SVGElement> result = new ArrayList<>();
        findElementsByTagNameRecursive(this, targetTagName != null ? targetTagName.toLowerCase() : "", result);
        return result;
    }

    private void findElementsByTagNameRecursive(SVGElement current, String target, List<SVGElement> result) {
        for (SVGElement child : current.getChildren()) {
            if ("*".equals(target) || child.getTagName().equalsIgnoreCase(target)) {
                result.add(child);
            }
            child.findElementsByTagNameRecursive(child, target, result);
        }
    }

    /**
     * Recursively retrieves an element by its 'id' attribute.
     *
     * @param targetId The ID to search for.
     * @return The matching SVGElement, or null if not found.
     */
    public SVGElement getElementById(String targetId) {
        if (targetId == null) {
            return null;
        }
        if (targetId.equals(this.getId())) {
            return this;
        }
        for (SVGElement child : children) {
            SVGElement match = child.getElementById(targetId);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * Recursively collects this element and all its descendant elements.
     *
     * @param collector List to accumulate elements into.
     */
    public void collectAllElements(List<SVGElement> collector) {
        collector.add(this);
        for (SVGElement child : children) {
            child.collectAllElements(collector);
        }
    }

    /**
     * Common presentation attribute accessors.
     */
    public String getFill() {
        return getAttribute("fill");
    }

    public String getStroke() {
        return getAttribute("stroke");
    }

    public double getStrokeWidth() {
        return getDoubleAttribute("stroke-width", 1.0);
    }

    public String getTransform() {
        return getAttribute("transform");
    }

    /**
     * Formats attributes into a clean string for display.
     *
     * @return Formatted attribute string.
     */
    public String formatAttributes() {
        if (attributes.isEmpty()) {
            return "(no attributes)";
        }
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Generates a readable, hierarchical ASCII tree representation of this element
     * and all of its descendants.
     *
     * @return Multi-line formatted tree string.
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, "", true, true);
        return sb.toString();
    }

    private void buildTreeString(StringBuilder sb, String prefix, boolean isTail, boolean isRoot) {
        if (isRoot) {
            sb.append("[").append(tagName).append("] ").append(formatAttributes()).append("\n");
        } else {
            sb.append(prefix)
              .append(isTail ? "└── [" : "├── [")
              .append(tagName)
              .append("] ")
              .append(formatAttributes())
              .append("\n");
        }

        for (int i = 0; i < children.size(); i++) {
            boolean childIsTail = (i == children.size() - 1);
            String childPrefix = isRoot ? "  " : prefix + (isTail ? "    " : "│   ");
            children.get(i).buildTreeString(sb, childPrefix, childIsTail, false);
        }
    }

    @Override
    public String toString() {
        return "<" + tagName + (getId() != null ? " id=\"" + getId() + "\"" : "") +
                " " + formatAttributes() +
                (children.isEmpty() ? "/>" : "> (" + children.size() + " children)");
    }
}

