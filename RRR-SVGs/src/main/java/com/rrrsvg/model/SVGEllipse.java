package com.rrrsvg.model;

/**
 * Represents an SVG &lt;ellipse&gt; element.
 * <p>
 * Encapsulates ellipse geometry attributes (cx, cy, rx, ry)
 * and styling attributes.
 * </p>
 */
public class SVGEllipse extends SVGElement {

    public static final String TAG_NAME = "ellipse";

    public SVGEllipse() {
        super(TAG_NAME);
    }

    public double getCx() {
        return getDoubleAttribute("cx", 0.0);
    }

    public double getCy() {
        return getDoubleAttribute("cy", 0.0);
    }

    public double getRx() {
        return getDoubleAttribute("rx", 0.0);
    }

    public double getRy() {
        return getDoubleAttribute("ry", 0.0);
    }
}

