package com.rrrsvg.model;

/**
 * Represents the root &lt;svg&gt; element of an SVG document.
 * <p>
 * Contains canvas-level attributes such as width, height, viewBox,
 * and namespaces, and acts as the root of the element hierarchy.
 * </p>
 */
public class SVGRoot extends SVGElement {

    public static final String TAG_NAME = "svg";

    public SVGRoot() {
        super(TAG_NAME);
    }

    /**
     * Returns the canvas width, falling back to viewBox width if width attribute is missing.
     *
     * @return Width as double, or 0.0 if not specified.
     */
    public double getWidth() {
        if (hasAttribute("width")) {
            return getDoubleAttribute("width", 0.0);
        }
        double[] vb = getViewBoxBounds();
        return vb != null ? vb[2] : 0.0;
    }

    /**
     * Returns the canvas height, falling back to viewBox height if height attribute is missing.
     *
     * @return Height as double, or 0.0 if not specified.
     */
    public double getHeight() {
        if (hasAttribute("height")) {
            return getDoubleAttribute("height", 0.0);
        }
        double[] vb = getViewBoxBounds();
        return vb != null ? vb[3] : 0.0;
    }

    /**
     * Returns the raw width attribute string (e.g. "100%", "500px", "500").
     *
     * @return Raw width string, or null if absent.
     */
    public String getWidthString() {
        return getAttribute("width");
    }

    /**
     * Returns the raw height attribute string (e.g. "100%", "400px", "400").
     *
     * @return Raw height string, or null if absent.
     */
    public String getHeightString() {
        return getAttribute("height");
    }

    /**
     * Returns the viewBox attribute string (e.g. "0 0 300 200").
     *
     * @return viewBox string, or null if absent.
     */
    public String getViewBox() {
        return getAttribute("viewBox");
    }

    /**
     * Parses the viewBox attribute into an array of doubles: [minX, minY, width, height].
     *
     * @return 4-element double array, or null if viewBox is missing or invalid.
     */
    public double[] getViewBoxBounds() {
        String vb = getViewBox();
        if (vb == null || vb.trim().isEmpty()) {
            return null;
        }
        String[] parts = vb.trim().split("[,\\s]+");
        if (parts.length >= 4) {
            try {
                return new double[] {
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
                };
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the XML namespace string.
     *
     * @return xmlns attribute value.
     */
    public String getXmlns() {
        return getAttribute("xmlns");
    }
}

