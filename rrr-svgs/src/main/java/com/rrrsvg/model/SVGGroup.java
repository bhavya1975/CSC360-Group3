package com.rrrsvg.model;

/**
 * Represents an SVG &lt;g&gt; (group) container element.
 * <p>
 * Used to group related SVG elements together, share styles and transformations,
 * and define hierarchical nesting within the document.
 * </p>
 */
public class SVGGroup extends SVGElement {

    public static final String TAG_NAME = "g";

    public SVGGroup() {
        super(TAG_NAME);
    }
}

