package com.rrrsvg.model;

/**
 * Represents an SVG &lt;line&gt; element.
 * <p>
 * Encapsulates straight-line geometry coordinates (x1, y1, x2, y2)
 * and stroke styling attributes.
 * </p>
 */
public class SVGLine extends SVGElement {

    public static final String TAG_NAME = "line";

    public SVGLine() {
        super(TAG_NAME);
    }

    public double getX1() {
        return getDoubleAttribute("x1", 0.0);
    }

    public double getY1() {
        return getDoubleAttribute("y1", 0.0);
    }

    public double getX2() {
        return getDoubleAttribute("x2", 0.0);
    }

    public double getY2() {
        return getDoubleAttribute("y2", 0.0);
    }
}

