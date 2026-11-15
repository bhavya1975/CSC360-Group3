package com.rrrsvg.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an SVG &lt;polyline&gt; element (an open series of connected straight line segments).
 * <p>
 * Encapsulates the 'points' attribute and provides parsed
 * point coordinates as {@link Point2D.Double}.
 * </p>
 */
public class SVGPolyline extends SVGElement {

    public static final String TAG_NAME = "polyline";

    public SVGPolyline() {
        super(TAG_NAME);
    }

    /**
     * Returns the raw 'points' attribute string.
     *
     * @return Points string (e.g. "20,20 40,25 60,40").
     */
    public String getPoints() {
        return getAttribute("points", "");
    }

    /**
     * Parses the 'points' attribute into a list of 2D coordinates.
     * Handles comma and whitespace delimited coordinate pairs.
     *
     * @return List of parsed Point2D.Double vertices.
     */
    public List<Point2D.Double> getPointsList() {
        List<Point2D.Double> list = new ArrayList<>();
        String raw = getPoints().trim();
        if (raw.isEmpty()) {
            return list;
        }

        String[] tokens = raw.split("[,\\s]+");
        for (int i = 0; i + 1 < tokens.length; i += 2) {
            try {
                double x = Double.parseDouble(tokens[i]);
                double y = Double.parseDouble(tokens[i + 1]);
                list.add(new Point2D.Double(x, y));
            } catch (NumberFormatException ignored) {
                // Ignore unparseable tokens
            }
        }
        return Collections.unmodifiableList(list);
    }
}

