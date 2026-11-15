package com.rrrsvg.model;

/**
 * Represents an SVG &lt;circle&gt; element.
 * <p>
 * Encapsulates circle geometry attributes (cx, cy, r)
 * and styling attributes.
 * </p>
 */
public class SVGCircle extends SVGElement {

    public static final String TAG_NAME = "circle";

    public SVGCircle() {
        super(TAG_NAME);
    }

    public double getCx() {
        return getDoubleAttribute("cx", 0.0);
    }

    public double getCy() {
        return getDoubleAttribute("cy", 0.0);
    }

    public double getR() {
        return getDoubleAttribute("r", 0.0);
    }
}

