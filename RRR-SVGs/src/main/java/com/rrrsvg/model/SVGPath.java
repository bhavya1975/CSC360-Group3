package com.rrrsvg.model;

/**
 * Represents an SVG &lt;path&gt; element.
 * <p>
 * Encapsulates the path definition string ('d' attribute), which defines
 * arbitrary vector paths comprising lines, curves, arcs, etc.
 * </p>
 */
public class SVGPath extends SVGElement {

    public static final String TAG_NAME = "path";

    public SVGPath() {
        super(TAG_NAME);
    }

    /**
     * Returns the 'd' path data attribute string.
     *
     * @return Path command string (e.g. "M 10 80 Q 95 10 180 80"), or empty string if not defined.
     */
    public String getD() {
        return getAttribute("d", "");
    }

    /**
     * Alias for {@link #getD()} to provide clear, readable code.
     *
     * @return Path command string.
     */
    public String getPathData() {
        return getD();
    }
}

