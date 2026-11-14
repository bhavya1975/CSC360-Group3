package com.rrrsvg.model;

/**
 * Represents a generic or unrecognized SVG element (e.g. &lt;defs&gt;, &lt;text&gt;, &lt;title&gt;, &lt;use&gt;).
 * <p>
 * Ensures that any valid XML/SVG element in a document is captured in the hierarchy
 * with all its attributes and children, preserving full document completeness.
 * </p>
 */
public class SVGGenericElement extends SVGElement {

    public SVGGenericElement(String tagName) {
        super(tagName);
    }
}

