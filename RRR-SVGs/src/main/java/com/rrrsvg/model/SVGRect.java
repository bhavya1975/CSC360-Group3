package com.rrrsvg.model;

/**
 * Represents an SVG &lt;rect&gt; element.
 * <p>
 * Encapsulates rectangle geometry attributes (x, y, width, height, rx, ry)
 * and styling attributes.
 * </p>
 */
public class SVGRect extends SVGElement {

    public static final String TAG_NAME = "rect";

    public SVGRect() {
        super(TAG_NAME);
    }

    public double getX() {
        return getDoubleAttribute("x", 0.0);
    }

    public double getY() {
        return getDoubleAttribute("y", 0.0);
    }

    public double getWidth() {
        return getDoubleAttribute("width", 0.0);
    }

    public double getHeight() {
        return getDoubleAttribute("height", 0.0);
    }

    public double getRx() {
        return getDoubleAttribute("rx", 0.0);
    }

    public double getRy() {
        return getDoubleAttribute("ry", 0.0);
    }
}

