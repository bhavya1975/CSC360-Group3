package com.rrrsvg.reader;

import java.util.List;
import java.util.Map;

/** One SVG element: its tag name, its attributes and its child elements. */
public record SvgNode(String name, Map<String, String> attributes, List<SvgNode> children) {

    public String attr(String key) {
        return attributes.get(key);
    }

    /** Reads a numeric attribute such as {@code width="100"} or {@code width="100px"}. */
    public double number(String key, double defaultValue) {
        String value = attributes.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim().replace("px", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
